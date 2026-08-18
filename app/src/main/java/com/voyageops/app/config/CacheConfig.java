package com.voyageops.app.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis-backed cache configuration with per-cache time-to-live.
 * <p>
 * Values are serialized as JSON with embedded type hints so cached objects
 * deserialize back to their declared types. Read-heavy, trip-scoped queries
 * (destination/activity lists, budget summary) are cached and evicted by trip
 * on every write.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_TRIP = "trip";
    public static final String CACHE_DESTINATIONS = "trip-destinations";
    public static final String CACHE_ACTIVITIES = "trip-activities";
    public static final String CACHE_EXPENSE_SUMMARY = "expense-summary";
    public static final String CACHE_USERS = "users";
    public static final String CACHE_WEATHER = "weather";
    public static final String CACHE_PLACES = "places";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, RedisCacheConfiguration> configurations = new HashMap<>();
        configurations.put(CACHE_TRIP, cacheConfig(Duration.ofMinutes(10)));
        configurations.put(CACHE_DESTINATIONS, cacheConfig(Duration.ofHours(1)));
        configurations.put(CACHE_ACTIVITIES, cacheConfig(Duration.ofHours(1)));
        configurations.put(CACHE_EXPENSE_SUMMARY, cacheConfig(Duration.ofMinutes(5)));
        configurations.put(CACHE_USERS, cacheConfig(Duration.ofMinutes(30)));
        configurations.put(CACHE_WEATHER, cacheConfig(Duration.ofHours(3)));
        configurations.put(CACHE_PLACES, cacheConfig(Duration.ofDays(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(configurations)
                .build();
    }

    private RedisCacheConfiguration cacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));
    }
}