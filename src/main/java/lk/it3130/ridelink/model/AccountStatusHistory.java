package lk.it3130.ridelink.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "account_status_history")
public class AccountStatusHistory {

    @Id
    private String id;

    private String accountId;
    private AccountStatus oldStatus;
    private AccountStatus newStatus;
    private String changedBy;
    private String reason;
    private Instant changedAt;
}
