package lk.ac.ridelink.drivervehicle.driver.service;

import java.time.LocalDate;
import lk.ac.ridelink.drivervehicle.driver.document.Driver;
import lk.ac.ridelink.drivervehicle.driver.document.DriverStatus;
import lk.ac.ridelink.drivervehicle.driver.dto.*;
import lk.ac.ridelink.drivervehicle.driver.exception.DriverConflictException;
import lk.ac.ridelink.drivervehicle.driver.exception.DriverNotFoundException;
import lk.ac.ridelink.drivervehicle.driver.repository.DriverRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class DriverService {
    private final DriverRepository repository;

    public DriverService(DriverRepository repository) {
        this.repository = repository;
    }

    public DriverResponse create(CreateDriverRequest request) {
        String accountId = request.accountId().strip();
        String licenseNumber = request.licenseNumber().strip();
        if (repository.existsByAccountId(accountId)) {
            throw new DriverConflictException("A driver already exists for this account.");
        }
        if (repository.existsByLicenseNumber(licenseNumber)) {
            throw new DriverConflictException("This license number is already registered.");
        }
        Driver driver = new Driver();
        driver.setAccountId(accountId);
        applyProfile(driver, request.firstName(), request.lastName(), request.phoneNumber(),
                licenseNumber, request.licenseExpiryDate());
        driver.setStatus(DriverStatus.PENDING);
        return response(repository.save(driver));
    }

    public DriverResponse get(String id) {
        return response(find(id));
    }

    public DriverPageResponse list(int page, int size, DriverStatus status, String accountId) {
        var pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
        String account = accountId == null || accountId.isBlank() ? null : accountId.strip();
        var results = account != null
                ? (status != null ? repository.findByAccountIdAndStatus(account, status, pageable)
                                  : repository.findByAccountId(account, pageable))
                : (status != null ? repository.findByStatus(status, pageable)
                                  : repository.findAll(pageable));
        return new DriverPageResponse(results.getContent().stream().map(DriverService::response).toList(),
                results.getNumber(), results.getSize(), results.getTotalElements(), results.getTotalPages());
    }

    public DriverResponse update(String id, UpdateDriverRequest request) {
        Driver driver = find(id);
        String licenseNumber = request.licenseNumber().strip();
        if (repository.existsByLicenseNumberAndIdNot(licenseNumber, id)) {
            throw new DriverConflictException("This license number is already registered.");
        }
        applyProfile(driver, request.firstName(), request.lastName(), request.phoneNumber(),
                licenseNumber, request.licenseExpiryDate());
        return response(repository.save(driver));
    }

    public DriverResponse updateStatus(String id, UpdateDriverStatusRequest request) {
        Driver driver = find(id);
        DriverStatus next = request.status();
        if (driver.getStatus() == next) {
            return response(driver);
        }
        if (next == DriverStatus.PENDING) {
            throw new DriverConflictException("A driver cannot return to PENDING status.");
        }
        if (next == DriverStatus.APPROVED && !driver.getLicenseExpiryDate().isAfter(LocalDate.now())) {
            throw new DriverConflictException("Approval requires an unexpired license.");
        }
        driver.setStatus(next);
        return response(repository.save(driver));
    }

    public void delete(String id) {
        repository.delete(find(id));
    }

    private Driver find(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new DriverNotFoundException("Driver not found."));
    }

    private static void applyProfile(Driver driver, String firstName, String lastName,
            String phoneNumber, String licenseNumber, LocalDate expiryDate) {
        driver.setFirstName(firstName.strip());
        driver.setLastName(lastName.strip());
        driver.setPhoneNumber(phoneNumber.strip());
        driver.setLicenseNumber(licenseNumber);
        driver.setLicenseExpiryDate(expiryDate);
    }

    private static DriverResponse response(Driver driver) {
        return new DriverResponse(driver.getId(), driver.getAccountId(), driver.getFirstName(),
                driver.getLastName(), driver.getPhoneNumber(), driver.getLicenseNumber(),
                driver.getLicenseExpiryDate(), driver.getStatus(), driver.getCreatedAt(), driver.getUpdatedAt());
    }
}
