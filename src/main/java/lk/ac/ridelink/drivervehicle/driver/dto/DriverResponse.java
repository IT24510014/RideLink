package lk.ac.ridelink.drivervehicle.driver.dto;

import java.time.Instant;
import java.time.LocalDate;
import lk.ac.ridelink.drivervehicle.driver.document.DriverStatus;

public record DriverResponse(String id, String accountId, String firstName, String lastName,
        String phoneNumber, String licenseNumber, LocalDate licenseExpiryDate,
        DriverStatus status, Instant createdAt, Instant updatedAt) {}
