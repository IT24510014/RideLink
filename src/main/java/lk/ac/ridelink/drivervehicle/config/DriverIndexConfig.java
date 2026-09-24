package lk.ac.ridelink.drivervehicle.config;

import lk.ac.ridelink.drivervehicle.driver.document.Driver;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class DriverIndexConfig {
    @Bean
    SmartInitializingSingleton driverIndexes(MongoTemplate template) {
        // Complete index creation before lifecycle components, including the web server, start.
        return () -> {
            var indexes = template.indexOps(Driver.class);
            indexes.createIndex(new Index().on("accountId", Sort.Direction.ASC)
                    .unique().named("driver_account_unique"));
            indexes.createIndex(new Index().on("licenseNumber", Sort.Direction.ASC)
                    .unique().named("driver_license_unique"));
        };
    }
}
