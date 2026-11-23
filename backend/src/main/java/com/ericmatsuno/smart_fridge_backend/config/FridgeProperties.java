package com.ericmatsuno.smart_fridge_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Type-safe settings bound from application.properties ("fridge.*").
 *
 * @param expiringSoonDays items expiring within this many days count as "use soon"
 */
@ConfigurationProperties(prefix = "fridge")
public record FridgeProperties(@DefaultValue("3") int expiringSoonDays) {
}
