package cl.duoc.barriodigital.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBindingRequest {

    @NotBlank(message = "El nombre de la cola o destino es obligatorio")
    private String destination;

    @NotBlank(message = "El nombre del exchange es obligatorio")
    private String exchange;

    @NotNull(message = "El routing key no puede ser nulo")
    private String routingKey;

    @Builder.Default
    private String destinationType = "QUEUE";
}
