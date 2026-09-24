package lk.ac.ridelink.drivervehicle.driver.dto;

import jakarta.validation.constraints.NotNull;
import lk.ac.ridelink.drivervehicle.driver.document.DriverStatus;

public record UpdateDriverStatusRequest(@NotNull DriverStatus status) {}
