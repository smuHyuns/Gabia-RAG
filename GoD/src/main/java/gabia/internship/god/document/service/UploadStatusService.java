package gabia.internship.god.document.service;

import gabia.internship.god.common.config.redis.UploadStatusProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UploadStatusService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final UploadStatusProperties props;

    /**
     * total-count : 임베딩된 전체 개수
     * parsing-count : 메시지큐에 축적된 전체 개수 ( 파싱 후 임베딩으로 전달된 개수 )
     * upload-count : 성공 개수 ( 최종 벡터DB 저장 )
     * embedding-fail : 임베딩 실패 개수
     * document-fail : 문서 삽입 실패 개수
     * pending-fail : 대기중인 실패 메시지 개수(미처리)
     * collection : 제공된 컬렉션명
     * email : 결과 발송 이메일 주소
     */

    /**
     * 초기화
     */
    public Mono<Void> initializeUploadStatus(String uploadId, String collectionName, String email) {
        return Mono.when(
                reactiveRedisTemplate.opsForValue().set(props.total(uploadId), "0"),
                reactiveRedisTemplate.opsForValue().set(props.parsing(uploadId), "0"),
                reactiveRedisTemplate.opsForValue().set(props.upload(uploadId), "0"),
                reactiveRedisTemplate.opsForValue().set(props.embeddingFail(uploadId), "0"),
                reactiveRedisTemplate.opsForValue().set(props.documentFail(uploadId), "0"),
                reactiveRedisTemplate.opsForValue().set(props.pendingFail(uploadId), "0"),
                reactiveRedisTemplate.opsForValue().set(props.collection(uploadId), collectionName),
                reactiveRedisTemplate.opsForValue().set(props.email(uploadId), email)
        ).then(); // 모든 작업이 끝나면 Mono<Void> 반환
    }


    /**
     * 전달받은 데이터 전체개수
     */
    public Mono<Void> increaseTotalCount(String uploadId, int num) {
        return reactiveRedisTemplate.opsForValue()
                .increment(props.total(uploadId), num)
                .then();
    }

    public Mono<Integer> getTotalCount(String uploadId) {
        return reactiveRedisTemplate.opsForValue()
                .get(props.total(uploadId))
                .map(Integer::parseInt)
                .defaultIfEmpty(0);
    }

    /**
     * parsing-count 전체개수
     */
    public Mono<Void> increaseParsingCount(String uploadId, int num) {
        return reactiveRedisTemplate.opsForValue()
                .increment(props.parsing(uploadId), num)
                .then();
    }

    public Mono<Integer> getParsingCount(String uploadId) {
        return reactiveRedisTemplate.opsForValue()
                .get(props.parsing(uploadId))
                .map(Integer::parseInt)
                .defaultIfEmpty(0);
    }

    /**
     * upload-count DB 삽입성공개수
     */
    public Mono<Void> increaseUploadCount(String uploadId, int count) {
        return reactiveRedisTemplate.opsForValue()
                .increment(props.upload(uploadId), count)
                .then();
    }

    public Mono<Integer> getUploadCount(String uploadId) {
        return reactiveRedisTemplate.opsForValue()
                .get(props.upload(uploadId))
                .map(Integer::parseInt)
                .defaultIfEmpty(0);
    }

    /**
     * pending 실패 대기 개수
     */
    public Mono<Void> increasePendingFailures(String uploadId) {
        return reactiveRedisTemplate.opsForValue()
                .increment(props.pendingFail(uploadId))
                .then();
    }

    public Mono<Void> decreasePendingFailures(String uploadId) {
        return getPendingFailures(uploadId)
                .flatMap(count -> {
                    if (count > 0) {
                        return reactiveRedisTemplate.opsForValue()
                                .decrement(props.pendingFail(uploadId))
                                .then();
                    } else {
                        return Mono.empty();
                    }
                });
    }


    public Mono<Integer> getPendingFailures(String uploadId) {
        return reactiveRedisTemplate.opsForValue()
                .get(props.pendingFail(uploadId))
                .map(Integer::parseInt)
                .defaultIfEmpty(0);
    }

    /**
     * embedding 실패 대기 개수
     */
    public Mono<Void> increaseEmbeddingFailures(String uploadId, int count) {
        return reactiveRedisTemplate.opsForValue()
                .increment(props.embeddingFail(uploadId), count)
                .then();
    }

    public Mono<Integer> getEmbeddingFailureCount(String uploadId) {
        return reactiveRedisTemplate.opsForValue()
                .get(props.embeddingFail(uploadId))
                .map(Integer::parseInt)
                .defaultIfEmpty(0);
    }

    /**
     * document 실패 대기 개수
     */
    public Mono<Void> increaseDocumentFailures(String uploadId, int count) {
        return reactiveRedisTemplate.opsForValue()
                .increment(props.documentFail(uploadId), count)
                .then();
    }

    public Mono<Integer> getDocumentFailureCount(String uploadId) {
        return reactiveRedisTemplate.opsForValue()
                .get(props.documentFail(uploadId))
                .map(Integer::parseInt)
                .defaultIfEmpty(0);
    }

    /**
     * 결과 상태 확인 :
     * 성공 + (실패 : 임베딩, 문서) = 총합 전처리 개수 && 실패 대기 X 여야 완료로 처리
     */
    public Mono<Boolean> isComplete(String uploadId) {
        return Mono.zip(
                getUploadCount(uploadId), // 업로드 성공개수
                getDocumentFailureCount(uploadId), // 업로드 실패개수
                getEmbeddingFailureCount(uploadId), // 임베딩 실패개수
                getParsingCount(uploadId), // 파싱(추적) 성공개수
                getPendingFailures(uploadId) // 실패 큐 업무 대기 수
        ).map(tuple -> {
            int upload = tuple.getT1();
            int fail = tuple.getT2() + tuple.getT3();
            int parsing = tuple.getT4();
            int pending = tuple.getT5();
            return (upload + fail == parsing) && pending == 0;
        });
    }

    /**
     * collection, email
     */
    public Mono<String> getCollection(String uploadId) {
        return reactiveRedisTemplate.opsForValue()
                .get(props.collection(uploadId))
                .defaultIfEmpty("");
    }

    public Mono<String> getEmail(String uploadId) {
        return reactiveRedisTemplate.opsForValue()
                .get(props.email(uploadId))
                .defaultIfEmpty("");
    }

    /**
     * 사용후 전체 삭제
     */
    public Mono<Void> clearAll(String uploadId) {
        return Flux.just(
                        props.total(uploadId),
                        props.parsing(uploadId),
                        props.upload(uploadId),
                        props.embeddingFail(uploadId),
                        props.documentFail(uploadId),
                        props.pendingFail(uploadId),
                        props.collection(uploadId),
                        props.email(uploadId)
                )
                .flatMap(reactiveRedisTemplate::delete)
                .then();
    }

    /**
     * retryCount에 따라서 업데이트 처리
     * retryCount == 0 이면 pending 처리
     * retryCount == 3 이면(DLQ 가서 종료됨) pending -- 처리 후 실패 개수 추가
     */
    public Mono<Void> updateEmbeddingPendingFailure(String uploadId, Integer retryCount, int size) {
        if (retryCount == 0) {
            return increasePendingFailures(uploadId);
        } else if (retryCount == 3) {
            if (size > 0) {
                return decreasePendingFailures(uploadId)
                        .then(increaseEmbeddingFailures(uploadId, size));
            } else {
                return decreasePendingFailures(uploadId);
            }
        }
        return Mono.empty();
    }

    public Mono<Void> updateDocumentPendingFailure(String uploadId, Integer retryCount, int size) {
        if (retryCount == 0) {
            return increasePendingFailures(uploadId);
        } else if (retryCount == 3) {
            if (size > 0) {
                return decreasePendingFailures(uploadId)
                        .then(increaseDocumentFailures(uploadId, size));
            } else {
                return decreasePendingFailures(uploadId);
            }
        }
        return Mono.empty();
    }


    /**
     * retryCount 확인
     */
    @SuppressWarnings("unchecked")
    public int getRetryCount(Map<String, Object> headers) {
        if (headers == null) return 0;

        Object xDeath = headers.get("x-death");
        if (!(xDeath instanceof List<?> xDeathList) || xDeathList.isEmpty()) return 0;

        Map<String, Object> latestDeath = (Map<String, Object>) xDeathList.get(0);
        Object count = latestDeath.get("count");

        if (count == null) return 0;

        try {
            if (count instanceof Number num) {
                return num.intValue();
            }
            return Integer.parseInt(count.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}
