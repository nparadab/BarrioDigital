package cl.duoc.barriodigital.requests.dto;

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
public class CreateRequestDto {

    @NotBlank(message = "El DNI/RUT del vecino es obligatorio")
    private String citizenDni;

    @NotBlank(message = "El nombre del vecino es obligatorio")
    private String citizenName;

    private String citizenEmail;
    private String citizenPhone;

    @NotNull(message = "El ID del tipo de trámite es obligatorio")
    private Long procedureTypeId;

    @NotBlank(message = "El nombre del trámite es obligatorio")
    private String procedureName;

    @NotBlank(message = "La descripción de la solicitud es obligatoria")
    private String description;

    @NotBlank(message = "La dirección de la solicitud es obligatoria")
    private String address;

    private String createdBy;
}
