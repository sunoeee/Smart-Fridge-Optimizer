package com.ericmatsuno.smart_fridge_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Services ask this Clock for "today" instead of calling LocalDate.now() directly.
 * In tests we swap in Clock.fixed(...), so date logic like "expires in 2 days" gives
 * the same answer no matter which day the tests run.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
