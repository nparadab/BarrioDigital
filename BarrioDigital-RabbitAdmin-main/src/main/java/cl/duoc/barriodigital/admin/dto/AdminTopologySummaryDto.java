package cl.duoc.barriodigital.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTopologySummaryDto {
    private String status;
    private LocalDateTime timestamp;
    private List<Map<String, Object>> predefinedQueues;
    private List<Map<String, Object>> predefinedExchanges;
    private List<Map<String, Object>> predefinedBindings;
}
