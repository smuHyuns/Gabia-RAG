package gabia.internship.god.document.controller;

import gabia.internship.god.document.dto.request.parsing.ParsingResult;
import gabia.internship.god.document.service.DocumentUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/documents")
@Slf4j
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentUploadService documentUploadService;

    @PostMapping
    public Mono<Void> uploadJson(@RequestBody ParsingResult result) {
        if (result.isFailed()) {
            log.warn("[UPLOAD][{}] 파싱 실패한 데이터입니다.", result.dataSet());
            return documentUploadService.sendFailMessage(result.dataSet(), result.email());
        }
        return documentUploadService.handleUpload(result);
    }
}
