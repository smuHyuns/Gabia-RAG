package gabia.internship.god.document.service;

import gabia.internship.god.common.config.rabbitmq.RabbitMQProperties;
import gabia.internship.god.common.config.webflux.BufferProperties;
import gabia.internship.god.common.message.DocumentToMailMessage;
import gabia.internship.god.common.util.JsonMessageConverter;
import gabia.internship.god.core.vectorstore.VectorStoreDocumentService;
import gabia.internship.god.common.message.JsonToParsingMessage;
import gabia.internship.god.common.message.EmbeddingToUploadMessage;
import gabia.internship.god.document.dto.request.message.DocParsedData;
import gabia.internship.god.document.dto.request.parsing.ParsingResult;
import gabia.internship.god.document.dto.request.vector.VectorDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentUploadService {

    private final UploadStatusService uploadStatusService;
    private final VectorStoreDocumentService<String, VectorDataDto> documentService;
    private final RabbitMQProperties rabbitProps;
    private final Sender sender;
    private final BufferProperties buffers;

    /**
     * 데이터 추적 초기화 -> dataSet 생성 -> 전달받은 데이터 크기 추적 -> 메시지 큐에 전달 -> 최종처리 위한 메시지 발송
     */
    public Mono<Void> handleUpload(ParsingResult result) {
        String uploadId = UUID.randomUUID().toString();
        String dataSet = result.dataSet();
        String email = result.email();
        List<DocParsedData> data = result.data();

        return uploadStatusService.initializeUploadStatus(uploadId, dataSet, email)
                .then(documentService.createDataSet(dataSet))
                .then(uploadStatusService.increaseTotalCount(uploadId, data.size())) //
                .thenMany(Flux.fromIterable(data)
                        .buffer(buffers.docsUpload())
                        .flatMap(batch -> sendBatchToQueue(uploadId, dataSet, email, batch)
                                .then(), 20)
                )
                .then(sendFinalMessage(uploadId, dataSet))
                .doOnSuccess(ignored -> {
                    log.info("[DOCUMENT] 모든 배치 MQ 전송 완료 - uploadId: {}, 총 건수: {}", uploadId, data.size());
                })
                .onErrorResume(error -> {
                    log.error("[DOCUMENT] MQ 전송 실패 - uploadId: {}, 에러: {}", uploadId, error.getMessage());
                    log.warn("[DOCUMENT] 실패로 인한 상태 초기화 완료 - uploadId: {}", uploadId);
                    return sendFailMessage(dataSet, email)
                            .then(uploadStatusService.clearAll(uploadId));
                });
    }

    private Mono<Void> sendBatchToQueue(String uploadId, String dataSet, String email, List<DocParsedData> batch) {
        JsonToParsingMessage message = new JsonToParsingMessage(uploadId, dataSet, email, batch);

        return sendMessage(message, rabbitProps.exchange().main(), rabbitProps.routing().workDocumentParsing())
                .doOnSuccess(ignored ->
                        log.info("[DOCUMENT] 배치 전송 완료 - uploadId: {}, batchSize: {}", uploadId, batch.size())
                ).doOnError(e ->
                        log.error("[DOCUMENT] 배치 전송 실패 - uploadId: {}, 에러: {}", uploadId, e.getMessage())
                );
    }

    private Mono<Void> sendFinalMessage(String uploadId, String collectionName) {
        EmbeddingToUploadMessage lastMessage = new EmbeddingToUploadMessage(collectionName, List.of(), uploadId, true);

        return sendMessage(lastMessage, rabbitProps.exchange().main(), rabbitProps.routing().workDocument())
                .doOnSuccess(ignored ->
                        log.info("[DOCUMENT][PARSING] 마지막 메시지 전송 완료 - uploadId: {}", uploadId)
                );
    }


    public Mono<Void> sendFailMessage(String collectionName, String email) {
        String uploadId = UUID.randomUUID().toString();
        DocumentToMailMessage failMessage = DocumentToMailMessage.builder()
                .uploadId(uploadId)
                .collectionName(collectionName)
                .email(email)
                .isSuccess(false)
                .build();
        return sendMessage(failMessage, rabbitProps.exchange().main(), rabbitProps.routing().workDocumentMail())
                .doOnSuccess(ignored ->
                        log.info("[DOCUMENT] 실패 메시지 전송 완료 - uploadId: {}", uploadId)
                );
    }

    private Mono<Void> sendMessage(Object messageObject, String exchange, String routingKey) {
        return Mono.fromCallable(() -> JsonMessageConverter.toBytes(messageObject))
                .flatMap(body -> {
                    OutboundMessage outbound = new OutboundMessage(
                            exchange,
                            routingKey,
                            body
                    );
                    return sender.send(Mono.just(outbound))
                            .retryWhen(
                                    reactor.util.retry.Retry.fixedDelay(3, Duration.ofMillis(200))
                                            .doBeforeRetry(rs -> log.warn("[DOCUMENT] 메시지 전송 재시도 - attempt {}", rs.totalRetries()))
                            )
                            .doOnError(error -> log.error("[DOCUMENT] 메시지 전송 재시도 실패", error));
                })
                .then();
    }

}
