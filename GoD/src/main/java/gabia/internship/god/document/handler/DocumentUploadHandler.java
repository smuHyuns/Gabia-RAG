package gabia.internship.god.document.handler;

import gabia.internship.god.common.config.rabbitmq.RabbitMQProperties;
import gabia.internship.god.common.handler.MessageHandler;
import gabia.internship.god.common.util.JsonMessageConverter;
import gabia.internship.god.core.vectorstore.VectorStoreDocumentService;
import gabia.internship.god.common.message.DocumentToMailMessage;
import gabia.internship.god.common.message.EmbeddingToUploadMessage;
import gabia.internship.god.document.dto.request.vector.VectorDataDto;
import gabia.internship.god.document.service.UploadStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentUploadHandler implements MessageHandler<EmbeddingToUploadMessage> {

    private final Sender sender;

    private final VectorStoreDocumentService<String, VectorDataDto> documentService;

    private final UploadStatusService uploadStatusService;

    private final RabbitMQProperties props;

    /**
     * 일반 메시지 업로드 처리
     * -> 성공시 기존 실패전적이 있을 시 ( retryCount > 0) 대기 실패 개수 감소 | upload-count 증가
     * -> 실패시 기존 실패전적에 따라 다른 처리 ( 0일시 pending 증가, 3일시 pending 감소 및 실패 추가 )
     * 마지막 메시지 처리
     * -> isComplete 가 true 가 될 때까지 대기
     */
    @Override
    public Mono<Void> handle(EmbeddingToUploadMessage message, Integer retryCount) {
        String uploadId = message.uploadId();
        int size = message.vectors().size();

        return Mono.defer(() -> {
                    if (!message.isLast()) {
                        return handleUploadBatch(message, retryCount);
                    }

                    // 마지막 메시지 로직 처리
                    return checkCompletion(uploadId);
                })
                .onErrorResume(error -> {
                    log.error("[DOCUMENT][uploadId: {}] 문서 처리 실패 - 재시도 횟수: {} - 에러: {}", uploadId, retryCount, error.getMessage());

                    Mono<Void> failUpdate;
                    if (message.isLast())
                        failUpdate = uploadStatusService.updateDocumentPendingFailure(uploadId, retryCount, size)
                                .doOnSuccess(v -> log.warn("[DOCUMENT] 실패 상태 업데이트 완료 - uploadId: {}", uploadId));
                    else failUpdate = Mono.empty();

                    return failUpdate.then(Mono.error(error));
                });
    }


    private Mono<Void> handleUploadBatch(EmbeddingToUploadMessage message, int retryCount) {
        String uploadId = message.uploadId();
        return documentService.uploadVectors(message.collectionName(), Flux.fromIterable(message.vectors()))
                .reduce(0, Integer::sum)
                .flatMap(count -> {
                    Mono<Void> updateStatus = uploadStatusService.increaseUploadCount(uploadId, count)
                            .then(Mono.fromRunnable(() ->
                                    log.info("[DOCUMENT][uploadId: {}] insert batch 성공 - 개수: {}", uploadId, count)
                            ));

                    if (retryCount > 0) {
                        return uploadStatusService.decreasePendingFailures(uploadId)
                                .doOnSuccess(success -> log.info("[DOCUMENT][uploadId: {}] 작업 성공으로 인한 실패 대기 개수 감소", uploadId))
                                .then(updateStatus);
                    }

                    return updateStatus;
                })
                .onErrorMap(error -> {
                    log.error("[DOCUMENT][uploadId: {}] insert 실패: {}", uploadId, error.getMessage());
                    return new RuntimeException("문서 벡터 업로드 실패", error);
                });
    }


    /**
     * 마지막 메시지 처리
     * -> 추적했던 개수들의 값을 정리하여 메일에 보낼 형식으로 만들어 전달
     * TODO:이 메시지는 실패해서는 안됨, 따라서 로직의 보강이 필요 -> 재시도횟수를 별도로 둔다던지..
     */
    private Mono<Void> handleFinalMessage(String uploadId) {
        return Mono.zip(
                uploadStatusService.getTotalCount(uploadId),
                uploadStatusService.getUploadCount(uploadId),
                uploadStatusService.getEmbeddingFailureCount(uploadId),
                uploadStatusService.getDocumentFailureCount(uploadId),
                uploadStatusService.getCollection(uploadId),
                uploadStatusService.getEmail(uploadId),
                uploadStatusService.getParsingCount(uploadId)
        ).flatMap(tuple -> {
            int totalCount = tuple.getT1(); // 데이터 전체 건수
            int uploadCount = tuple.getT2(); // 벡터 DB 업로드 개수
            int embeddingFailure = tuple.getT3(); // 임베딩 실패
            int documentFailure = tuple.getT4(); // 문서 업로드 실패
            String collectionName = tuple.getT5();
            String email = tuple.getT6();
            int parsingCount = tuple.getT7();

            DocumentToMailMessage mailMessage = DocumentToMailMessage.builder()
                    .uploadId(uploadId)
                    .collectionName(collectionName)
                    .email(email)
                    .total(totalCount)
                    .parsing(parsingCount)
                    .upload(uploadCount)
                    .embeddingFail(embeddingFailure)
                    .parsingFail(totalCount - parsingCount)
                    .uploadFail(documentFailure)
                    .isSuccess(true)
                    .build();

            return Mono.fromCallable(() -> JsonMessageConverter.toBytes(mailMessage))
                    .flatMap(body -> {
                        OutboundMessage outbound = new OutboundMessage(
                                props.exchange().main(),
                                props.routing().workDocumentMail(),
                                body
                        );
                        return sender.send(Mono.just(outbound))
                                .doOnSuccess(ignored ->
                                        log.info("[DOCUMENT][EMAIL] {}로 전송 요청 완료", email)
                                );
                    })
                    .then(uploadStatusService.clearAll(uploadId));
        });
    }

    /**
     * upload 완료를 감시하는 비동기 메서드
     */
    private Mono<Void> checkCompletion(String uploadId) {
        return Flux.interval(Duration.ofSeconds(2))
                .concatMap(tick -> Mono.defer(() -> uploadStatusService.isComplete(uploadId)))
                .filter(Boolean::booleanValue)
                .next()
                .flatMap(complete -> {
                    log.info("[DOCUMENT] 완료 감지됨 - handleFinalMessage 호출 시작: uploadId={}", uploadId);
                    return handleFinalMessage(uploadId);
                })
                .timeout(Duration.ofMinutes(25))
                .onErrorResume(error -> {
                    log.error("[DOCUMENT] 모니터링 실패 - uploadId: {}, 에러={}", uploadId, error.getMessage());
                    return Mono.error(new RuntimeException("checkCompletion 실패"));
                });
    }


}
