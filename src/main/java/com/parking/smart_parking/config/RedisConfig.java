package com.parking.smart_parking.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 配置类
 *
 * 功能：
 * 1. 开启 Spring Cache 注解支持（@EnableCaching）
 * 2. 配置 RedisCacheManager：使用 JSON 序列化缓存值，方便查看和跨语言兼容
 * 3. 配置 StringRedisTemplate：用于直接操作 Redis String 类型（如验证码）
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * 缓存默认过期时间：10 分钟
     */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    /**
     * 配置 Redis 缓存管理器
     *
     * 说明：
     * - key 采用 String 序列化，方便在 Redis 客户端直接阅读
     * - value 采用 GenericJackson2JsonRedisSerializer 序列化，自动处理 List/Map/POJO
     * - 默认 TTL 10 分钟，可在 @Cacheable 注解中单独覆盖
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }

    /**
     * 配置 StringRedisTemplate
     *
     * 用于直接执行 Redis 命令（如 SET key value EX seconds）。
     * StringRedisTemplate 默认使用 String 序列化，适合验证码、计数器等简单键值。
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        return template;
    }
}
