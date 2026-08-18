package com.voyageops.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * Enables method-level resilience for external integrations.
 * <p>
 * Adapters annotated with {@code @Retryable} and {@code @ConcurrencyLimit}
 * are wrapped so transient upstream failures are retried with backoff while
 * concurrent calls are throttled. Retry configuration lives on the adapter
 * methods themselves, keeping policies co-located with each provider.
 */
@Configuration
@EnableResilientMethods
public class ResilienceConfig {
}