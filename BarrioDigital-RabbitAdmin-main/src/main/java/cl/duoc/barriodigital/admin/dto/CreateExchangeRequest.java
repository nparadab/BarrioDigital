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
public class CreateExchangeRequest {

    @NotBlank(message = "El nombre del exchange es obligatorio y no puede estar vacío")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre del exchange contiene caracteres no válidos (solo se permiten alfanuméricos, punto, guion y guion bajo)")
    private String name;

    @NotBlank(message = "El tipo de exchange es obligatorio")
    @Pattern(regexp = "^(?i)(direct|topic|fanout|headers)$", message = "Tipo de exchange inválido. Tipos permitidos: direct, topic, fanout, headers")
    private String type;

    @Builder.Default
    private boolean durable = true;

    @Builder.Default
    private boolean autoDelete = false;
}
