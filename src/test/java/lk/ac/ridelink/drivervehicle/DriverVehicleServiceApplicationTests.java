package lk.ac.ridelink.drivervehicle;

import org.junit.jupiter.api.Test;
import lk.ac.ridelink.drivervehicle.driver.controller.DriverController;
import lk.ac.ridelink.drivervehicle.driver.service.DriverService;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// Load only the web slice and an empty test configuration, never the Atlas configuration.
@WebMvcTest(controllers = DriverController.class,
        properties = "spring.config.location=classpath:/driver-test.properties")
class DriverVehicleServiceApplicationTests {

    @MockitoBean
    private DriverService driverService;

	@Test
	void contextLoads() {
	}

}
