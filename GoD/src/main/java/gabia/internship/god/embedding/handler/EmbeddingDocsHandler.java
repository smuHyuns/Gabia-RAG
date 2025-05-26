package gabia.internship.god.embedding.handler;

import gabia.internship.god.common.config.rabbitmq.RabbitMQProperties;
import gabia.internship.god.common.config.webflux.BufferProperties;
import gabia.internship.god.common.exception.CommonException;
import gabia.internship.god.common.handler.MessageHandler;
import gabia.internship.god.common.util.JsonMessageConverter;
import gabia.internship.god.common.message.ParsingToEmbeddingMessage;
import gabia.internship.god.document.dto.request.message.CsvRowMessage;
import gabia.internship.god.common.message.EmbeddingToUploadMessage;
import gabia.internship.god.document.dto.request.vector.VectorDataDto;
import gabia.internship.god.document.service.UploadStatusService;
import gabia.internship.god.embedding.dto.response.openAI.EmbeddingResponseDto;
import gabia.internship.god.embedding.service.OpenAIEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingDocsHandler implements MessageHandler<ParsingToEmbeddingMessage> {

    private final OpenAIEmbeddingService embeddingService;
    private final UploadStatusService uploadStatusService;
    private final RabbitMQProperties props;
    private final Sender sender;
    private final BufferProperties buffers;

    /**
     * 전달받은 데이터를 임베딩하여 문서 업로드 큐에 전달
     * -> 성공시 기존 실패전적이 있을 시 ( retryCount > 0) 대기 실패 개수 감소
     * -> 실패시 기존 실패전적에 따라 다른 처리 ( 0일시 pending 증가, 3일시 pending 감소 및 실패 추가 )
     */
    @Override
    public Mono<Void> handle(ParsingToEmbeddingMessage message, Integer retryCount) {
        String uploadId = message.uploadId();
        List<CsvRowMessage> allRows = message.rows();

        return processEmbeddingBatches(message)
                .then(retryCount > 0
                        ? uploadStatusService.decreasePendingFailures(uploadId) : Mono.empty())
                .onErrorResume(error -> {
                    log.error("[EMBEDDING][uploadId: {}] 임베딩 실패 - 재시도 횟수: {} - 에러: {}", uploadId, retryCount, error.getMessage());
                    return uploadStatusService.updateEmbeddingPendingFailure(uploadId, retryCount, allRows.size())
                            .then(Mono.error(error));
                });
    }

    private Mono<Void> processEmbeddingBatches(ParsingToEmbeddingMessage message) {
        String uploadId = message.uploadId();

        return Flux.fromIterable(message.rows())
                .buffer(buffers.embeddingDocs())
                .concatMap(batch -> {
                    List<String> answers = batch.stream()
                            .map(row -> row.payload().get("answer"))
                            .toList();

                    ParsingToEmbeddingMessage batchMessage = new ParsingToEmbeddingMessage(
                            uploadId,
                            message.collectionName(),
                            batch
                    );

                    return embeddingService.embeddingData(answers)
                            .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                                    .maxBackoff(Duration.ofSeconds(3)))
                            .flatMap(response -> sendToDocumentQueue(batchMessage, response));
                })
                .then();
    }


    private Mono<Void> sendToDocumentQueue(ParsingToEmbeddingMessage batchMessage, EmbeddingResponseDto response) {
        List<VectorDataDto> vectors = new ArrayList<>();
        for (int i = 0; i < response.data().size(); i++) {
            CsvRowMessage row = batchMessage.rows().get(i);
            vectors.add(VectorDataDto.of(
                    row.docId(),
                    response.data().get(i).embedding(),
                    new HashMap<>(row.payload())
            ));
        }

        EmbeddingToUploadMessage msg = new EmbeddingToUploadMessage(
                batchMessage.collectionName(), vectors, batchMessage.uploadId(), false
        );

        byte[] body = JsonMessageConverter.toBytes(msg);

        OutboundMessage outbound = new OutboundMessage(
                props.exchange().main(),
                props.routing().workDocument(),
                body
        );

        return sender.send(Mono.just(outbound))
                .doOnSuccess(success ->
                        log.info("[EMBEDDING] {}개 벡터 전송 완료 - uploadId: {}", vectors.size(), batchMessage.uploadId())
                )
                .onErrorMap(e -> {
                    if (e instanceof HttpMessageNotReadableException) {
                        return new CommonException("응답 역직렬화 실패", e, HttpStatus.BAD_REQUEST);
                    } else {
                        return new CommonException("예상치 못한 에러", e, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                })
                .then();
    }
}
