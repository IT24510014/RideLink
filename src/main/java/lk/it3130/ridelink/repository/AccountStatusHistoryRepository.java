package lk.it3130.ridelink.repository;

import lk.it3130.ridelink.model.AccountStatusHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountStatusHistoryRepository extends MongoRepository<AccountStatusHistory, String> {

    List<AccountStatusHistory> findByAccountIdOrderByChangedAtDesc(String accountId);
}
