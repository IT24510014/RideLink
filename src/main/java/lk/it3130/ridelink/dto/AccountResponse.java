package lk.it3130.ridelink.dto;

import lk.it3130.ridelink.model.AccountStatus;
import lk.it3130.ridelink.model.Profile;
import lk.it3130.ridelink.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Role role;
    private AccountStatus status;
    private Profile profile;
    private Instant createdAt;
    private Instant updatedAt;
}
