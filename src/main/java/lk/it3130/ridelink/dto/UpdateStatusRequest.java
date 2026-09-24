package lk.it3130.ridelink.dto;

import jakarta.validation.constraints.NotNull;
import lk.it3130.ridelink.model.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private AccountStatus status;

    private String reason;
}
