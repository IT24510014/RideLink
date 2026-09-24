package lk.ac.ridelink.drivervehicle.driver.document;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import static org.assertj.core.api.Assertions.*;

class DriverMappingTest {
    @Test
    void recognizesVersionAndDistinguishesNewFromPersistedDrivers() {
        var context = new MongoMappingContext();
        context.setSimpleTypeHolder(MongoCustomConversions.create(adapter -> {}).getSimpleTypeHolder());
        var entity = context.getRequiredPersistentEntity(Driver.class);
        assertThat(entity.hasVersionProperty()).isTrue();
        assertThat(entity.getRequiredVersionProperty().getName()).isEqualTo("version");

        Driver driver = new Driver();
        assertThat(entity.isNew(driver)).isTrue();
        driver.setId("driver-1");
        driver.setVersion(0L);
        assertThat(entity.isNew(driver)).isFalse();
    }
}
