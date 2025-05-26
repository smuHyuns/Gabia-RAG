package gabia.internship.god.unit.document.controller;

import gabia.internship.god.core.vectorstore.VectorStoreDocumentService;
import gabia.internship.god.document.controller.DocumentController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static gabia.internship.god.integration.document.converter.CsvPointConverterUtils.makeMockCsvContent;
import static org.mockito.Mockito.*;

@WebFluxTest(DocumentController.class)
class DocumentControllerUnitTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private VectorStoreDocumentService documentService;

    private static final String DATA_SET = "test-dataset";
    private static final String BASE_URL = "/api/documents";
    private static final String FILE_NAME = "test.csv";

    private String getUrl() {
        return BASE_URL + "?dataSet=" + DATA_SET;
    }

    private BodyInserters.MultipartInserter makeMultipartRequestBody(boolean includeFile) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        if (includeFile) {
            byte[] csvBytes = makeMockCsvContent().getBytes(StandardCharsets.UTF_8);
            builder.part("file", new ByteArrayResource(csvBytes))
                    .filename(FILE_NAME)
                    .contentType(MediaType.TEXT_PLAIN);
        }
        builder.part("dataSet", DATA_SET);
        return BodyInserters.fromMultipartData(builder.build());
    }

    @Test
    @DisplayName("CSV 업로드 성공")
    void uploadCsv_success() {
        when(documentService.saveCsvToDataSet(any(FilePart.class), eq(DATA_SET)))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri(getUrl())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(makeMultipartRequestBody(true))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("CSV 업로드 실패 - file 파라미터 누락")
    void uploadCsv_missingFileParam() {
        webTestClient.post()
                .uri(getUrl())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(makeMultipartRequestBody(false))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("CSV 업로드 실패 - 내부 서버 오류")
    void uploadCsv_internalServerError() {
        when(documentService.saveCsvToDataSet(any(FilePart.class), eq(DATA_SET)))
                .thenReturn(Mono.error(new RuntimeException("Internal Error")));

        webTestClient.post()
                .uri(getUrl())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(makeMultipartRequestBody(true))
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
