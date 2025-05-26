package gabia.internship.god.common.constants;

public class Constants {

    public static final String PROMPTING_SYSTEM = "system";

    public static final String PROMPTING_USER = "user";

    public static final String OPENAI_CHAT_PATH = "/chat/completions";

    public static final String OPENAI_EMBEDDING_PATH = "/embeddings";

    public static final String DISTANCE = "Cosine";

    public static final int SIZE = 3072; // Vector Store 컬렉션의 크기(임베딩 개수)

    public static final int K = 5; // Vector Store에서 찾는 문서의 개수

    public static final float THRESHOLD = 0.45f; // 문서의 유효성 판별하는 기준 (0.35 이상의 유사도일시 유효한 문서로 판단)

    public static final String QDRANT_COLLECTION_PATH = "/collections";

    public static final String QDRANT_POINT_PATH = "/points";

    public static final String QDRANT_SEARCH_PATH = "/search";

    public static final String RABBITMQ_MESSAGE_HEADER_TTL = "x-message-ttl";

    public static final String RABBITMQ_MESSAGE_HEADER_DLX = "x-dead-letter-exchange";

    public static final String RABBITMQ_MESSAGE_HEADER_DLX_ROUTING = "x-dead-letter-routing-key";
}
