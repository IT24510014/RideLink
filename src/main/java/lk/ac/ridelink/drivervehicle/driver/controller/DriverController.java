package lk.ac.ridelink.drivervehicle.driver.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lk.ac.ridelink.drivervehicle.driver.document.DriverStatus;
import lk.ac.ridelink.drivervehicle.driver.dto.*;
import lk.ac.ridelink.drivervehicle.driver.service.DriverService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {
    private final DriverService service;

    public DriverController(DriverService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DriverResponse> create(@Valid @RequestBody CreateDriverRequest request) {
        DriverResponse driver = service.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(driver.id()).toUri();
        return ResponseEntity.created(location).body(driver);
    }

    @GetMapping("/{id}")
    public DriverResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @GetMapping
    public DriverPageResponse list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) DriverStatus status,
            @RequestParam(required = false) String accountId) {
        return service.list(page, size, status, accountId);
    }

    @PutMapping("/{id}")
    public DriverResponse update(@PathVariable String id,
            @Valid @RequestBody UpdateDriverRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public DriverResponse updateStatus(@PathVariable String id,
            @Valid @RequestBody UpdateDriverStatusRequest request) {
        return service.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
