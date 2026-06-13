package com.Basisttha.IronHold.Config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
<<<<<<< Updated upstream
=======
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
>>>>>>> Stashed changes

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {

        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder()
<<<<<<< Updated upstream
=======
                .enableDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build())
>>>>>>> Stashed changes
                .enableSpringCacheNullValueSupport()
                .build();

        //default config, fallback for any cache not explicitly configured
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer));

        // per-cache overrides. same serializer, different TTL
        RedisCacheConfiguration downloadUrlConfig = defaultConfig
                .entryTtl(Duration.ofMinutes(14));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("downloadUrls", downloadUrlConfig);

        return RedisCacheManager.builder(factory)
<<<<<<< Updated upstream
=======
		.withInitialCacheConfigurations(cacheConfigs)
>>>>>>> Stashed changes
                .cacheDefaults(defaultConfig)
                .build();
    }
}