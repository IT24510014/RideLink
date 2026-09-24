package lk.ac.ridelink.drivervehicle.config;

import lk.ac.ridelink.drivervehicle.driver.document.Driver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DriverIndexConfigTest {
    @Test
    void createsBothUniqueIndexesBeforeLifecycleStart() {
        MongoTemplate template = mock(MongoTemplate.class);
        IndexOperations indexes = mock(IndexOperations.class);
        SmartLifecycle lifecycle = mock(SmartLifecycle.class);
        when(template.indexOps(Driver.class)).thenReturn(indexes);
        when(lifecycle.isAutoStartup()).thenReturn(true);

        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(MongoTemplate.class, () -> template);
            context.registerBean(SmartLifecycle.class, () -> lifecycle);
            context.register(DriverIndexConfig.class);
            context.refresh();

            var definitions = ArgumentCaptor.forClass(IndexDefinition.class);
            var order = inOrder(indexes, lifecycle);
            order.verify(indexes, times(2)).createIndex(definitions.capture());
            order.verify(lifecycle).start();

            var accountIndex = definitions.getAllValues().get(0);
            assertThat(accountIndex.getIndexKeys()).containsEntry("accountId", 1).hasSize(1);
            assertThat(accountIndex.getIndexOptions())
                    .containsEntry("unique", true).containsEntry("name", "driver_account_unique");
            var licenseIndex = definitions.getAllValues().get(1);
            assertThat(licenseIndex.getIndexKeys()).containsEntry("licenseNumber", 1).hasSize(1);
            assertThat(licenseIndex.getIndexOptions())
                    .containsEntry("unique", true).containsEntry("name", "driver_license_unique");
        }
    }

    @Test
    void indexFailurePreventsLifecycleStart() {
        MongoTemplate template = mock(MongoTemplate.class);
        IndexOperations indexes = mock(IndexOperations.class);
        SmartLifecycle lifecycle = mock(SmartLifecycle.class);
        when(template.indexOps(Driver.class)).thenReturn(indexes);
        when(indexes.createIndex(any())).thenThrow(new DataAccessResourceFailureException("Index creation failed"));

        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(MongoTemplate.class, () -> template);
            context.registerBean(SmartLifecycle.class, () -> lifecycle);
            context.register(DriverIndexConfig.class);

            assertThatThrownBy(context::refresh).isInstanceOf(DataAccessResourceFailureException.class);
            verify(lifecycle, never()).start();
        }
    }
}
