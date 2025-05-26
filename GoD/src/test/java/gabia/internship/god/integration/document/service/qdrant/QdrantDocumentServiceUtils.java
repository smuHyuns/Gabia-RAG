package gabia.internship.god.integration.document.service.qdrant;

import gabia.internship.god.document.dto.request.vector.VectorDataDto;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static gabia.internship.god.integration.search.service.qdrant.QdrantSearchServiceUtils.makeVectorList;

public class QdrantDocumentServiceUtils {
    public static VectorDataDto makeMockVectorDataDto() {
        List<Double> vectors = makeVectorList();
        return VectorDataDto.of(
                1,
                vectors,
                Map.of(
                        "subject", "테스트 제목",
                        "content", "테스트 콘텐츠 입니다"
                )
        );
    }

    public static FilePart createTestFilePart(String filename, String content) {
        return new FilePart() {
            @Override
            public String filename() {
                return filename;
            }

            @Override
            public Mono<Void> transferTo(Path dest) {
                return Mono.empty();
            }

            @Override
            public String name() {
                return "file";
            }

            @Override
            public HttpHeaders headers() {
                return new HttpHeaders();
            }

            @Override
            public Flux<DataBuffer> content() {
                DataBuffer buffer = new DefaultDataBufferFactory().wrap(content.getBytes(StandardCharsets.UTF_8));
                return Flux.just(buffer);
            }
        };
    }

}
