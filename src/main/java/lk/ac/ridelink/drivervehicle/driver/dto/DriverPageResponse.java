package lk.ac.ridelink.drivervehicle.driver.dto;

import java.util.List;

public record DriverPageResponse(List<DriverResponse> items, int page, int size,
        long totalElements, int totalPages) {}
