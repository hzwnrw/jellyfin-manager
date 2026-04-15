package com.hzwnrw.jellyfin.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RedisConfigTest {

    @Test
    void redisConnectionFactoryUsesConfiguredHostPortAndPassword() {
        RedisConfig redisConfig = new RedisConfig();
        ReflectionTestUtils.setField(redisConfig, "redisHost", "redis.internal");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6380);
        ReflectionTestUtils.setField(redisConfig, "redisPassword", "secret");

        LettuceConnectionFactory connectionFactory = redisConfig.redisConnectionFactory();

        assertEquals("redis.internal", connectionFactory.getHostName());
        assertEquals(6380, connectionFactory.getPort());
        assertEquals("secret", connectionFactory.getPassword());
    }

    @Test
    void redisConnectionFactoryLeavesPasswordUnsetWhenBlank() {
        RedisConfig redisConfig = new RedisConfig();
        ReflectionTestUtils.setField(redisConfig, "redisHost", "localhost");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6379);
        ReflectionTestUtils.setField(redisConfig, "redisPassword", "");

        LettuceConnectionFactory connectionFactory = redisConfig.redisConnectionFactory();

        assertEquals("localhost", connectionFactory.getHostName());
        assertEquals(6379, connectionFactory.getPort());
        assertNull(connectionFactory.getPassword());
    }

    @Test
    void stringRedisTemplateInitializesWithProvidedConnectionFactory() {
        RedisConfig redisConfig = new RedisConfig();
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", 6379);

        StringRedisTemplate template = redisConfig.stringRedisTemplate(connectionFactory);

        assertSame(connectionFactory, template.getConnectionFactory());
        assertNotNull(template.getKeySerializer());
        assertNotNull(template.getValueSerializer());
        assertNotNull(template.getHashKeySerializer());
        assertNotNull(template.getHashValueSerializer());
    }

    @Test
    void cacheManagerAppliesExpectedDefaultCacheConfiguration() {
        RedisConfig redisConfig = new RedisConfig();
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", 6379);

        RedisCacheManager cacheManager = redisConfig.cacheManager(connectionFactory);
        RedisCache cache = (RedisCache) cacheManager.getCache("users");

        assertNotNull(cache);

        RedisCacheConfiguration configuration = cache.getCacheConfiguration();

        assertNotNull(configuration);
        assertEquals(Duration.ofHours(1), configuration.getTtl());
        assertTrue(configuration.usePrefix());
        assertNotNull(configuration.getKeySerializationPair());
        assertNotNull(configuration.getValueSerializationPair());
    }
}
