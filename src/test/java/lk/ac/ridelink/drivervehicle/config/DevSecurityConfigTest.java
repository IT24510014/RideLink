package lk.ac.ridelink.drivervehicle.config;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lk.ac.ridelink.drivervehicle.driver.controller.DriverController;
import lk.ac.ridelink.drivervehicle.driver.document.DriverStatus;
import lk.ac.ridelink.drivervehicle.driver.dto.DriverPageResponse;
import lk.ac.ridelink.drivervehicle.driver.dto.DriverResponse;
import lk.ac.ridelink.drivervehicle.driver.service.DriverService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DriverController.class,
        properties = "spring.config.location=classpath:/driver-test.properties")
@ActiveProfiles("dev")
@Import({DevSecurityConfig.class, DevSecurityConfigTest.SecurityProbeController.class})
class DevSecurityConfigTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private DriverService service;

    private DriverResponse response(DriverStatus status) {
        return new DriverResponse("driver-1", "account-1", "Nimal", "Silva",
                "+94771234567", "B12345", LocalDate.now().plusYears(1),
                status, Instant.now(), Instant.now());
    }

    private String profile() {
        return """
                {"firstName":"Nimal","lastName":"Silva","phoneNumber":"+94771234567",
                 "licenseNumber":"B12345","licenseExpiryDate":"%s"}
                """.formatted(LocalDate.now().plusYears(1));
    }

    @Test
    void anonymousCanListDrivers() throws Exception {
        when(service.list(0, 20, null, null))
                .thenReturn(new DriverPageResponse(List.of(response(DriverStatus.PENDING)), 0, 20, 1, 1));
        mvc.perform(get("/api/v1/drivers"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value("driver-1"));
    }

    @Test
    void anonymousCanReadDriver() throws Exception {
        when(service.get("driver-1")).thenReturn(response(DriverStatus.PENDING));
        mvc.perform(get("/api/v1/drivers/driver-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("driver-1"));
    }

    @Test
    void anonymousCanCreateWithoutCsrf() throws Exception {
        when(service.create(any())).thenReturn(response(DriverStatus.PENDING));
        mvc.perform(post("/api/v1/drivers").contentType(MediaType.APPLICATION_JSON)
                        .content(profile().replace("{", "{\"accountId\":\"account-1\",")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value("driver-1"));
        verify(service).create(any());
    }

    @Test
    void anonymousCanUpdateWithoutCsrf() throws Exception {
        when(service.update(eq("driver-1"), any())).thenReturn(response(DriverStatus.PENDING));
        mvc.perform(put("/api/v1/drivers/driver-1").contentType(MediaType.APPLICATION_JSON).content(profile()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("driver-1"));
        verify(service).update(eq("driver-1"), any());
    }

    @Test
    void anonymousCanChangeStatusWithoutCsrf() throws Exception {
        when(service.updateStatus(eq("driver-1"), any())).thenReturn(response(DriverStatus.SUSPENDED));
        mvc.perform(patch("/api/v1/drivers/driver-1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));
        verify(service).updateStatus(eq("driver-1"), any());
    }

    @Test
    void anonymousCanDeleteWithoutCsrf() throws Exception {
        mvc.perform(delete("/api/v1/drivers/driver-1")).andExpect(status().isNoContent());
        verify(service).delete("driver-1");
    }

    @Test
    void anonymousCanReachHealthPath() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(content().string("health"));
    }

    @Test
    void otherEndpointRequiresAuthentication() throws Exception {
        mvc.perform(get("/test/protected")).andExpect(status().isUnauthorized());
        mvc.perform(get("/test/protected").with(user("test-principal")))
                .andExpect(status().isOk()).andExpect(content().string("protected"));
    }

    @Test
    void similarPathsRemainProtected() throws Exception {
        mvc.perform(get("/api/v1/drivers-private")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health/details")).andExpect(status().isUnauthorized());
    }

    // Test-only endpoints prove authorization rules without loading MongoDB or Actuator infrastructure.
    @RestController
    static class SecurityProbeController {
        @GetMapping("/test/protected")
        String protectedEndpoint() {
            return "protected";
        }

        @GetMapping("/actuator/health")
        String health() {
            return "health";
        }
    }
}
