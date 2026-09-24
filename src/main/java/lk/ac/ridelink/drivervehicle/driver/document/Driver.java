package lk.ac.ridelink.drivervehicle.driver.document;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "drivers")
public class Driver {
    @Id
    private String id;
    @Version
    private Long version;
    private String accountId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String licenseNumber;
    private LocalDate licenseExpiryDate;
    private DriverStatus status;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
