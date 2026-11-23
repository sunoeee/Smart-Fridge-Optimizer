package com.ericmatsuno.smart_fridge_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Starting the app at all is a real check: Flyway must apply V1-V3 to an empty Postgres,
 * and then Hibernate's ddl-auto=validate must agree that every entity matches its table.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SmartFridgeBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
