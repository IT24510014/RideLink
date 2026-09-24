package lk.ac.ridelink.drivervehicle.driver.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.*;

public record UpdateDriverRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Pattern(regexp = "\\+[1-9][0-9]{7,14}",
                message = "must use international format, for example +94771234567") String phoneNumber,
        @NotBlank @Size(max = 100) String licenseNumber,
        @NotNull @Future LocalDate licenseExpiryDate) {}
