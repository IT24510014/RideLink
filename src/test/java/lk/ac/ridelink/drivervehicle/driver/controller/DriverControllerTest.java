package lk.ac.ridelink.drivervehicle.driver.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lk.ac.ridelink.drivervehicle.config.DevSecurityConfig;
import lk.ac.ridelink.drivervehicle.driver.document.DriverStatus;
import lk.ac.ridelink.drivervehicle.driver.dto.*;
import lk.ac.ridelink.drivervehicle.driver.exception.*;
import lk.ac.ridelink.drivervehicle.driver.service.DriverService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DriverController.class,
        properties = "spring.config.location=classpath:/driver-test.properties")
@WithMockUser
@Import(DevSecurityConfig.class)
class DriverControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private DriverService service;

    private DriverResponse response() {
        return response(DriverStatus.PENDING);
    }

    private DriverResponse response(DriverStatus status) {
        return new DriverResponse("driver-1", "account-1", "Nimal", "Silva",
                "+94771234567", "B12345", LocalDate.now().plusYears(1),
                status, Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private String profile() {
        return """
                {"firstName":"Nimal","lastName":"Silva","phoneNumber":"+94771234567",
                 "licenseNumber":"B12345","licenseExpiryDate":"%s"}
                """.formatted(LocalDate.now().plusYears(1));
    }

    private String registration() {
        return profile().replace("{", "{\"accountId\":\"account-1\",");
    }

    @Test
    void createsDriverWithLocation() throws Exception {
        when(service.create(any())).thenReturn(response());
        mvc.perform(post("/api/v1/drivers").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(registration()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/drivers/driver-1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getsDriver() throws Exception {
        when(service.get("driver-1")).thenReturn(response());
        mvc.perform(get("/api/v1/drivers/driver-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accountId").value("account-1"))
                .andExpect(jsonPath("$.version").doesNotExist());
    }

    @Test
    void listsDriversWithDefaults() throws Exception {
        when(service.list(0, 20, null, null))
                .thenReturn(new DriverPageResponse(List.of(response()), 0, 20, 1, 1));
        mvc.perform(get("/api/v1/drivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("driver-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void forwardsListFilters() throws Exception {
        when(service.list(1, 5, DriverStatus.APPROVED, "account-1"))
                .thenReturn(new DriverPageResponse(List.of(), 1, 5, 0, 0));
        mvc.perform(get("/api/v1/drivers").param("page", "1").param("size", "5")
                        .param("status", "APPROVED").param("accountId", "account-1"))
                .andExpect(status().isOk());
        verify(service).list(1, 5, DriverStatus.APPROVED, "account-1");
    }

    @Test
    void updatesProfile() throws Exception {
        when(service.update(eq("driver-1"), any())).thenReturn(response());
        mvc.perform(put("/api/v1/drivers/driver-1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(profile()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("driver-1"));
    }

    @Test
    void updatesStatus() throws Exception {
        when(service.updateStatus(eq("driver-1"), any())).thenReturn(response(DriverStatus.SUSPENDED));
        mvc.perform(patch("/api/v1/drivers/driver-1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
        verify(service).updateStatus("driver-1", new UpdateDriverStatusRequest(DriverStatus.SUSPENDED));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "PATCH", "DELETE"})
    void concurrentModificationReturnsSafeConflict(String method) throws Exception {
        var conflict = new OptimisticLockingFailureException("internal database version detail");
        String path = "/api/v1/drivers/driver-1";
        String body = profile();
        switch (method) {
            case "PUT" -> when(service.update(eq("driver-1"), any())).thenThrow(conflict);
            case "PATCH" -> {
                when(service.updateStatus(eq("driver-1"), any())).thenThrow(conflict);
                path += "/status";
                body = "{\"status\":\"SUSPENDED\"}";
            }
            case "DELETE" -> doThrow(conflict).when(service).delete("driver-1");
            default -> throw new AssertionError("Unexpected method: " + method);
        }
        mvc.perform(request(HttpMethod.valueOf(method), path).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "The driver was changed by another request. Reload the driver and try again."));
    }

    @Test
    void deletesWithNoContent() throws Exception {
        mvc.perform(delete("/api/v1/drivers/driver-1").with(csrf()))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).delete("driver-1");
    }

    @Test
    void deleteMissingReturnsNotFound() throws Exception {
        doThrow(new DriverNotFoundException("Driver not found.")).when(service).delete("missing");
        mvc.perform(delete("/api/v1/drivers/missing").with(csrf()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void returnsNotFoundProblem() throws Exception {
        when(service.get("missing")).thenThrow(new DriverNotFoundException("Driver not found."));
        mvc.perform(get("/api/v1/drivers/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Driver not found."));
    }

    @Test
    void rejectsMissingFields() throws Exception {
        mvc.perform(post("/api/v1/drivers").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.accountId").exists())
                .andExpect(jsonPath("$.errors.licenseExpiryDate").exists());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsInvalidPhoneAndExpiredLicense() throws Exception {
        String invalid = registration().replace("+94771234567", "invalid")
                .replace(LocalDate.now().plusYears(1).toString(), LocalDate.now().minusDays(1).toString());
        mvc.perform(post("/api/v1/drivers").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phoneNumber").exists())
                .andExpect(jsonPath("$.errors.licenseExpiryDate").exists());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"status\":\"UNKNOWN\"}", "not-json"})
    void rejectsInvalidStatusBody(String body) throws Exception {
        mvc.perform(patch("/api/v1/drivers/driver-1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=abc", "status=UNKNOWN"})
    void rejectsInvalidListParameters(String query) throws Exception {
        mvc.perform(get("/api/v1/drivers?" + query)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void mapsBusinessConflict() throws Exception {
        when(service.create(any())).thenThrow(new DriverConflictException("Account already registered."));
        mvc.perform(post("/api/v1/drivers").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(registration()))
                .andExpect(status().isConflict());
    }

    @Test
    void mapsDatabaseUniquenessConflictWithoutExposingDetails() throws Exception {
        when(service.create(any())).thenThrow(new DuplicateKeyException("internal database detail"));
        mvc.perform(post("/api/v1/drivers").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(registration()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An account or license number is already registered."));
    }

    @Test
    void unexpectedFailureReturnsGenericProblem() throws Exception {
        when(service.get("driver-1")).thenThrow(new IllegalStateException("private detail"));
        mvc.perform(get("/api/v1/drivers/driver-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }

    @Test
    void securityStillRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/drivers").with(anonymous()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void securityStillRequiresCsrfForWrites() throws Exception {
        mvc.perform(delete("/api/v1/drivers/driver-1")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
