package lk.ac.ridelink.drivervehicle.driver.repository;

import lk.ac.ridelink.drivervehicle.driver.document.Driver;
import lk.ac.ridelink.drivervehicle.driver.document.DriverStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DriverRepository extends MongoRepository<Driver, String> {
    boolean existsByAccountId(String accountId);
    boolean existsByLicenseNumber(String licenseNumber);
    boolean existsByLicenseNumberAndIdNot(String licenseNumber, String id);
    Page<Driver> findByStatus(DriverStatus status, Pageable pageable);
    Page<Driver> findByAccountId(String accountId, Pageable pageable);
    Page<Driver> findByAccountIdAndStatus(String accountId, DriverStatus status, Pageable pageable);
}
