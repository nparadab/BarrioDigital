package cl.duoc.barriodigital.requests.dto;

import cl.duoc.barriodigital.requests.model.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusDto {

    @NotNull(message = "El nuevo estado es obligatorio")
    private RequestStatus status;

    private String assignedCrew;
    private String resolutionNotes;
    private String performedBy;
    private String userRole;
    private String comments;
}
