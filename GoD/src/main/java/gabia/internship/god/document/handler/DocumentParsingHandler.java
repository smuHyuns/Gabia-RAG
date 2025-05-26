package gabia.internship.god.document.handler;

import gabia.internship.god.common.config.rabbitmq.RabbitMQProperties;
import gabia.internship.god.common.handler.MessageHandler;
import gabia.internship.god.common.message.JsonToParsingMessage;
import gabia.internship.god.common.message.ParsingToEmbeddingMessage;
import gabia.internship.god.common.util.JsonMessageConverter;
import gabia.internship.god.document.dto.request.message.*;
import gabia.internship.god.document.service.UploadStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParsingHandler implements MessageHandler<JsonToParsingMessage> {

    private final UploadStatusService uploadStatusService;
    private final RabbitMQProperties props;
    private final Sender sender;

    /**
     * 전달받은 데이터 임베딩 큐로 전달
     * -> 성공시 기존 실패전적이 있을 시 ( retryCount > 0) 대기 실패 개수 감소 | 파싱 개수 추가
     * -> 실패시 기존 실패전적이 없을 시 ( retryCount == 0) 대기 실패 증가
     * -> 실패 개수가 최고 한도에 도달했을 시 ( retryCount == 3) 대기 실패 개수 감소 (더이상 처리되지 않으므로)
     */
    @Override
    public Mono<Void> handle(JsonToParsingMessage message, Integer retryCount) {
        String uploadId = message.uploadId();
        String collectionName = message.dataSet();
        List<DocParsedData> data = message.data();

        return sendBatchToQueue(uploadId, collectionName, data)
                .then(uploadStatusService.increaseParsingCount(uploadId, data.size()))
                .then(handleRetrySuccess(uploadId, retryCount))
                .onErrorResume(error -> handleRetryFailure(uploadId, retryCount, error));
    }


    private Mono<Void> sendBatchToQueue(String uploadId, String collectionName, List<DocParsedData> data) {
        List<CsvRowMessage> rows = data.stream()
                .map(doc -> new CsvRowMessage(
                        doc.docId(),
                        Map.of("question", doc.question(), "answer", doc.answer())
                ))
                .toList();

        ParsingToEmbeddingMessage message = new ParsingToEmbeddingMessage(uploadId, collectionName, rows);

        return Mono.fromCallable(() -> JsonMessageConverter.toBytes(message))
                .flatMap(body -> {
                    OutboundMessage outbound = new OutboundMessage(
                            props.exchange().main(),
                            props.routing().workEmbeddingCsv(),
                            body
                    );
                    return sender.send(Mono.just(outbound))
                            .doOnSuccess(ignored ->
                                    log.info("[DOCUMENT][BATCH] MQ 전송 완료 - 업로드 ID: {}, 컬렉션: {}, 메시지 수: {}",
                                            uploadId, collectionName, rows.size())
                            );
                });
    }

    private Mono<Void> handleRetrySuccess(String uploadId, int retryCount) {
        if (retryCount > 0) {
            return uploadStatusService.decreasePendingFailures(uploadId);
        }
        return Mono.empty();
    }


    private Mono<Void> handleRetryFailure(String uploadId, int retryCount, Throwable error) {
        if (retryCount == 0) {
            return uploadStatusService.increasePendingFailures(uploadId)
                    .then(Mono.error(new RuntimeException("문서 벡터 업로드 실패", error)));
        } else if (retryCount == 3) {
            return uploadStatusService.decreasePendingFailures(uploadId)
                    .then(Mono.error(new RuntimeException("문서 벡터 업로드 실패", error)));
        } else {
            return Mono.error(new RuntimeException("문서 벡터 업로드 실패", error));
        }
    }
}
