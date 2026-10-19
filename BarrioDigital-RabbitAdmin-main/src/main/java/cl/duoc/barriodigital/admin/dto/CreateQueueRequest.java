package cl.duoc.barriodigital.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQueueRequest {

    @NotBlank(message = "El nombre de la cola es obligatorio y no puede estar vacío")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre de la cola contiene caracteres no válidos (solo se permiten alfanuméricos, punto, guion y guion bajo)")
    private String name;

    @Builder.Default
    private boolean durable = true;

    @Builder.Default
    private boolean exclusive = false;

    @Builder.Default
    private boolean autoDelete = false;

    private String deadLetterExchange;

    private String deadLetterRoutingKey;

    private Integer messageTtl;
}
