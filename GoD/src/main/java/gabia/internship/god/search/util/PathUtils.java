package gabia.internship.god.search.util;

import gabia.internship.god.common.constants.Constants;

public class PathUtils {

    public static String makeDataSetPath(String dataSet) {
        return Constants.QDRANT_COLLECTION_PATH + "/" + dataSet;
    }

    public static String makeUploadVectorsPath(String dataSet) {
        return Constants.QDRANT_COLLECTION_PATH + "/" + dataSet + Constants.QDRANT_POINT_PATH;
    }

    public static String makeSearchSimilarData(String dataSet) {
        return Constants.QDRANT_COLLECTION_PATH + "/" + dataSet + Constants.QDRANT_POINT_PATH + Constants.QDRANT_SEARCH_PATH;
    }

}
