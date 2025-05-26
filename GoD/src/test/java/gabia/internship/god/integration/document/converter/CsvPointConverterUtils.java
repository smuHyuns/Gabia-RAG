package gabia.internship.god.integration.document.converter;

public class CsvPointConverterUtils {

    public static String makeMockCsvContent() {
        return """
            doc_id,question,answer
            1,테스트 질문입니다,테스트 콘텐츠 입니다
            """;
    }

    public static String makeMockCsvContent400Err(){
        return """
            doc_id,question,answer
            1,테스트 질문입니다,
            """;
    }
}