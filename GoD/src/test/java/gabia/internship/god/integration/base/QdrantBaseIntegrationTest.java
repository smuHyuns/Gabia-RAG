package gabia.internship.god.integration.base;

import gabia.internship.god.document.dto.request.qdrant.UploadPointsDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.qdrant.QdrantContainer;

import java.util.HashMap;
import java.util.Map;

import static gabia.internship.god.integration.search.service.qdrant.QdrantSearchServiceUtils.makeTestPoint;

@Testcontainers
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
public class QdrantBaseIntegrationTest {

    protected final String COLLECTION_NAME = "thisIsOnlyForTest";

    @Container
    static protected QdrantContainer qdrant =
            new QdrantContainer("qdrant/qdrant:v1.7.4")
                    .withExposedPorts(6333);

    @Autowired
    private WebClient vectorStoreWebClient;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("vector-store.base-url", () ->
                "http://" + qdrant.getHost() + ":" + qdrant.getMappedPort(6333));
    }

    @BeforeAll
    void setUp() {
        if (!qdrant.isRunning()) {
            qdrant.start();
        }
        createCollection();
        insertTestPoint();
    }

    @AfterAll
    void tearDown() {
        deleteCollection();
        if (qdrant.isRunning()) {
            qdrant.stop();
        }
    }

    protected void createCollection() {
        Map<String, Object> vectors = new HashMap<>();
        vectors.put("size", 1536);
        vectors.put("distance", "Cosine");

        Map<String, Object> body = new HashMap<>();
        body.put("vectors", vectors);

        vectorStoreWebClient.put()
                .uri("/collections/" + COLLECTION_NAME)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    protected void insertTestPoint() {
        UploadPointsDto points = makeTestPoint();

        vectorStoreWebClient.put()
                .uri("/collections/" + COLLECTION_NAME + "/points")
                .bodyValue(points)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    protected void deleteCollection() {
        vectorStoreWebClient.delete()
                .uri("/collections/" + COLLECTION_NAME)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
