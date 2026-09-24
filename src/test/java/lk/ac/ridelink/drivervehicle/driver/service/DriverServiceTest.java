package lk.ac.ridelink.drivervehicle.driver.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lk.ac.ridelink.drivervehicle.driver.document.Driver;
import lk.ac.ridelink.drivervehicle.driver.document.DriverStatus;
import lk.ac.ridelink.drivervehicle.driver.dto.*;
import lk.ac.ridelink.drivervehicle.driver.exception.*;
import lk.ac.ridelink.drivervehicle.driver.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {
    @Mock
    private DriverRepository repository;
    private DriverService service;
    private Driver driver;

    @BeforeEach
    void setUp() {
        service = new DriverService(repository);
        driver = new Driver();
        driver.setId("driver-1");
        driver.setAccountId("account-1");
        driver.setFirstName("Nimal");
        driver.setLastName("Silva");
        driver.setPhoneNumber("+94771234567");
        driver.setLicenseNumber("B12345");
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(1));
        driver.setStatus(DriverStatus.PENDING);
        driver.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
    }

    private CreateDriverRequest createRequest() {
        return new CreateDriverRequest(" account-1 ", " Nimal ", " Silva ",
                "+94771234567", " B12345 ", LocalDate.now().plusYears(1));
    }

    private UpdateDriverRequest updateRequest() {
        return new UpdateDriverRequest("Kamal", "Perera", "+94771234568",
                " B67890 ", LocalDate.now().plusYears(2));
    }

    @Test
    void createNormalizesFieldsAndAssignsPending() {
        when(repository.save(any(Driver.class))).thenAnswer(invocation -> {
            Driver saved = invocation.getArgument(0);
            saved.setId("generated-id");
            return saved;
        });
        var result = service.create(createRequest());
        assertThat(result.id()).isEqualTo("generated-id");
        assertThat(result.accountId()).isEqualTo("account-1");
        assertThat(result.firstName()).isEqualTo("Nimal");
        assertThat(result.lastName()).isEqualTo("Silva");
        assertThat(result.licenseNumber()).isEqualTo("B12345");
        assertThat(result.status()).isEqualTo(DriverStatus.PENDING);
    }

    @Test
    void rejectsDuplicateAccount() {
        when(repository.existsByAccountId("account-1")).thenReturn(true);
        assertThatThrownBy(() -> service.create(createRequest())).isInstanceOf(DriverConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsDuplicateLicense() {
        when(repository.existsByLicenseNumber("B12345")).thenReturn(true);
        assertThatThrownBy(() -> service.create(createRequest())).isInstanceOf(DriverConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void readsDriver() {
        when(repository.findById("driver-1")).thenReturn(Optional.of(driver));
        assertThat(service.get("driver-1").accountId()).isEqualTo("account-1");
    }

    @Test
    void missingDriverFailsEveryIdOperation() {
        assertThatThrownBy(() -> service.get("missing")).isInstanceOf(DriverNotFoundException.class);
        assertThatThrownBy(() -> service.update("missing", updateRequest()))
                .isInstanceOf(DriverNotFoundException.class);
        assertThatThrownBy(() -> service.updateStatus("missing",
                new UpdateDriverStatusRequest(DriverStatus.APPROVED)))
                .isInstanceOf(DriverNotFoundException.class);
        assertThatThrownBy(() -> service.delete("missing")).isInstanceOf(DriverNotFoundException.class);
        verify(repository, never()).delete(any(Driver.class));
    }

    @Test
    void updatePreservesIdentityStatusAndCreationTime() {
        driver.setStatus(DriverStatus.APPROVED);
        when(repository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(repository.save(driver)).thenReturn(driver);
        var result = service.update("driver-1", updateRequest());
        assertThat(result.firstName()).isEqualTo("Kamal");
        assertThat(result.licenseNumber()).isEqualTo("B67890");
        assertThat(result.id()).isEqualTo("driver-1");
        assertThat(result.accountId()).isEqualTo("account-1");
        assertThat(result.status()).isEqualTo(DriverStatus.APPROVED);
        assertThat(result.createdAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void updateRejectsAnotherDriversLicense() {
        when(repository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(repository.existsByLicenseNumberAndIdNot("B67890", "driver-1")).thenReturn(true);
        assertThatThrownBy(() -> service.update("driver-1", updateRequest()))
                .isInstanceOf(DriverConflictException.class);
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"PENDING,APPROVED", "PENDING,SUSPENDED", "APPROVED,SUSPENDED", "SUSPENDED,APPROVED"})
    void permitsStatusTransitions(DriverStatus from, DriverStatus to) {
        driver.setStatus(from);
        when(repository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(repository.save(driver)).thenReturn(driver);
        assertThat(service.updateStatus("driver-1", new UpdateDriverStatusRequest(to)).status()).isEqualTo(to);
    }

    @Test
    void rejectsReturnToPending() {
        driver.setStatus(DriverStatus.APPROVED);
        when(repository.findById("driver-1")).thenReturn(Optional.of(driver));
        assertThatThrownBy(() -> service.updateStatus("driver-1",
                new UpdateDriverStatusRequest(DriverStatus.PENDING))).isInstanceOf(DriverConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsApprovalWithExpiredLicense() {
        driver.setLicenseExpiryDate(LocalDate.now());
        when(repository.findById("driver-1")).thenReturn(Optional.of(driver));
        assertThatThrownBy(() -> service.updateStatus("driver-1",
                new UpdateDriverStatusRequest(DriverStatus.APPROVED))).isInstanceOf(DriverConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void repeatedStatusDoesNotWrite() {
        when(repository.findById("driver-1")).thenReturn(Optional.of(driver));
        assertThat(service.updateStatus("driver-1",
                new UpdateDriverStatusRequest(DriverStatus.PENDING)).status()).isEqualTo(DriverStatus.PENDING);
        verify(repository, never()).save(any());
    }

    @Test
    void deletesExistingDriver() {
        when(repository.findById("driver-1")).thenReturn(Optional.of(driver));
        service.delete("driver-1");
        verify(repository).delete(driver);
    }

    @Test
    void listsWithBothFiltersAndPagination() {
        when(repository.findByAccountIdAndStatus(eq("account-1"), eq(DriverStatus.PENDING), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(driver), invocation.getArgument(2), 1));
        var result = service.list(0, 20, DriverStatus.PENDING, " account-1 ");
        assertThat(result.items()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
    }

    @Test
    void listsWithoutFilters() {
        when(repository.findAll(any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(driver), invocation.getArgument(0), 1));
        assertThat(service.list(0, 20, null, null).items()).hasSize(1);
    }
}
