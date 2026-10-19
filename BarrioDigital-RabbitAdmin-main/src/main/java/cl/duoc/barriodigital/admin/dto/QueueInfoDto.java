package cl.duoc.barriodigital.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueInfoDto {
    private String name;
    private Integer messageCount;
    private Integer consumerCount;
    private Map<String, Object> arguments;
    private boolean exists;
}
