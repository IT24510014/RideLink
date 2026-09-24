package lk.it3130.ridelink.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile {

    private LocalDate dateOfBirth;
    private String gender;
    private String profilePictureUrl;
    private String address;
    private String emergencyContact;
}
