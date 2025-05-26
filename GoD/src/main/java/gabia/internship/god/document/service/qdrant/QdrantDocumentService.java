package gabia.internship.god.document.service.qdrant;


import gabia.internship.god.common.config.webflux.BufferProperties;
import gabia.internship.god.core.vectorstore.VectorStoreDocumentService;
import gabia.internship.god.search.util.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import gabia.internship.god.document.dto.request.vector.VectorDataDto;
import gabia.internship.god.document.dto.request.qdrant.UploadPointsDto;
import gabia.internship.god.document.dto.request.qdrant.CreateCollectionRequestDto;
import reactor.core.scheduler.Schedulers;

import static gabia.internship.god.document.dto.request.qdrant.UploadPointsDto.makeUploadPointsDto;
import static gabia.internship.god.document.dto.request.qdrant.CreateCollectionRequestDto.makeCreateCollectionRequestDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantDocumentService implements VectorStoreDocumentService<String, VectorDataDto> {
    private final WebClient vectorStoreWebClient;
    private final BufferProperties buffers;

    @Override
    public Mono<Boolean> exists(String collectionName) {
        return vectorStoreWebClient.get()
                .uri(PathUtils.makeDataSetPath(collectionName))
                .retrieve()
                .toBodilessEntity()
                .map(response -> true)
                .onErrorResume(e -> {
                    if (e instanceof WebClientResponseException.NotFound) {
                        return Mono.just(false);
                    } else {
                        log.warn("[DOCUMENT] Error checking collection '{}': {}", collectionName, e.toString());
                        return Mono.error(e);
                    }
                });
    }

    @Override
    public Mono<Void> createDataSet(String collectionName) {
        return exists(collectionName)
                .flatMap(exist -> {
                    if (exist) {
                        // 해당 컬렉션이 이미 존재한다면 -> 생성하지 않고, 기존 컬렉션에 추가 저장
                        log.info("[DOCUMENT] 이미 생성된 컬렉션입니다, 컬렉션 생성을 생략합니다");
                        return Mono.empty();
                    }

                    // 해당 컬렉션이 존재하지 않음 -> 새로운 컬렉션을 생성
                    CreateCollectionRequestDto requestBody = makeCreateCollectionRequestDto();

                    return vectorStoreWebClient.put()
                            .uri(PathUtils.makeDataSetPath(collectionName))
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(Void.class)
                            .onErrorResume(e -> {
                                log.error("[DOCUMENT] 컬렉션 생성 중 에러 발생, {}", collectionName);
                                return Mono.error(new RuntimeException("[DOCUMENT] 컬렉션 생성 중 에러 발생"));
                            });

                });
    }

    @Override
    public Flux<Integer> uploadVectors(String collectionName, Flux<VectorDataDto> pointsFlux) {
        return pointsFlux
                .buffer(buffers.qdrantDocs())
                .parallel(6) // 병렬 스트림 생성
                .runOn(Schedulers.boundedElastic()) // 병렬 작업용 스레드풀
                .flatMap(batch -> {
                    UploadPointsDto body = makeUploadPointsDto(batch);
                    long start = System.currentTimeMillis();

                    return vectorStoreWebClient.put()
                            .uri(PathUtils.makeUploadVectorsPath(collectionName))
                            .bodyValue(body)
                            .retrieve()
                            .bodyToMono(Void.class)
                            .doOnSuccess(v -> {
                                long duration = System.currentTimeMillis() - start;
                                log.info("[DOCUMENT] Qdrant 업로드 성공 (batch 시작 doc_id={}): {}개, 처리시간: {}ms",
                                        batch.get(0).id(), batch.size(), duration);
                            })
                            .thenReturn(batch.size())
                            .onErrorMap(e -> {
                                log.error("[DOCUMENT] Qdrant 업로드 실패 (batch 시작 doc_id={}): {}", batch.get(0).id(), e.getMessage());
                                return new RuntimeException("Qdrant 업로드 실패", e);
                            });

                })
                .sequential(); // 다시 Flux로 병합
    }


}
