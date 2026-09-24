package lk.it3130.ridelink.repository;

import lk.it3130.ridelink.model.Account;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends MongoRepository<Account, String> {

    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);
}
