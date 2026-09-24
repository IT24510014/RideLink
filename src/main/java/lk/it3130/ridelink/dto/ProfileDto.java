package lk.it3130.ridelink.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {

    private LocalDate dateOfBirth;
    private String gender;
    private String profilePictureUrl;
    private String address;
    private String emergencyContact;
}
