package gabia.internship.god.document.dto.request.vector;


import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record VectorDataDto(
        Integer id,
        double[] vector,
        Map<String, Object> payload
) {
   public static VectorDataDto of(Integer docId, List<Double> vector, Map<String, Object> payload) {
       return VectorDataDto.builder()
               .id(docId)
               .vector(toPrimitiveDoubleArray(vector))
               .payload(payload)
               .build();
   }

    private static double[] toPrimitiveDoubleArray(List<Double> list) {
        double[] result = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }

}
