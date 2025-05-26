package gabia.internship.god.document.handler;

import gabia.internship.god.common.handler.MessageHandler;
import gabia.internship.god.common.message.DocumentToMailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentMailHandler implements MessageHandler<DocumentToMailMessage> {

    private final JavaMailSender mailSender;


    @Override
    public Mono<Void> handle(DocumentToMailMessage message, Integer retryCount) {
        // 실패 메시지 처리
        if (!message.isSuccess()) return sendFailMessage(message);
        // 성공 메시지 처리
        return sendEmail(message);
    }

    /**
     * 이메일 발송
     * -> msg에 담긴 정보들을 추룰하여 메시지 포맷으로 만들어 전송
     * TODO: 메시지 발송이 실패 (3회) 일시 추후 어떻게 처리를 할 것인지?
     */
    public Mono<Void> sendEmail(DocumentToMailMessage msg) {
        return Mono.fromRunnable(() -> {
                    SimpleMailMessage message = new SimpleMailMessage();
                    String body = String.format("""
                                    <%s 데이터 업로드 작업 결과>
                                    - 요청 업로드 데이터 : %d 건
                                    - 유효 데이터 : %d 건
                                    - DB 업로드 성공 개수 : %d 건
                                    - DB 업로드 실패 개수 : %d 건
                                    
                                    <실패 내역>
                                    - 유효하지 않은 데이터 : %d 건
                                    - 임베딩 실패 : %d 건
                                    - 업로드 실패 : %d 건
                                    """,
                            msg.collectionName(), msg.total(), msg.parsing(), msg.upload(), msg.total() - msg.upload(),
                            msg.total() - msg.parsing(), msg.embeddingFail(), msg.uploadFail());
                    message.setTo(msg.email());
                    if (msg.total() == msg.parsing()) {
                        message.setSubject(msg.collectionName() + " 작업 완료 및 성공 안내 드립니다");
                    } else {
                        message.setSubject(msg.collectionName() + " 작업 완료 및 부분 성공 안내 드립니다");
                    }
                    message.setText(body);
                    mailSender.send(message);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then()
                .doOnSuccess(v -> log.info("[MAIL][uploadId: {}] 이메일 발송 성공", msg.uploadId()))
                .onErrorMap(error -> {
                    log.error("[MAIL][uploadId : {}] 문서 발송 실패: {}", msg.uploadId(), error.getMessage());
                    return new RuntimeException("이메일 발송 실패", error);
                });
    }


    private Mono<Void> sendFailMessage(DocumentToMailMessage msg) {
        return Mono.fromRunnable(() -> {
                    SimpleMailMessage message = new SimpleMailMessage();
                    String body = String.format("""
                            문서파싱이 실패하여 작업을 진행할 수 없습니다.
                            다시 요청주시기 바랍니다.
                            감사합니다.
                            """);
                    message.setTo(msg.email());
                    message.setSubject(msg.collectionName() + " 작업 실패 안내 드립니다");
                    message.setText(body);
                    mailSender.send(message);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then()
                .doOnSuccess(v -> log.info("[MAIL][uploadId: {}] 이메일 발송 성공", msg.uploadId()))
                .onErrorMap(error -> {
                    log.error("[MAIL][uploadId : {}] 문서 발송 실패: {}", msg.uploadId(), error.getMessage());
                    return new RuntimeException("이메일 발송 실패", error);
                });
    }

}