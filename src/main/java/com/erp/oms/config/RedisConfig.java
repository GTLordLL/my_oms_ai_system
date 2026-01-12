package com.erp.oms.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // --- 核心修改部分开始 ---
        ObjectMapper om = new ObjectMapper();

        // 1. 注册 JavaTimeModule 以支持 LocalDate, LocalDateTime 等 Java 8 时间类型
        om.registerModule(new JavaTimeModule());

        // 2. 禁用将日期序列化为时间戳（否则会变成 [2026,3,18]，Python 认不出来）
        // 禁用后会变成标准的 ISO 格式字符串 "2026-03-18"
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 保持你原有的可见性配置
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 保持类型信息，方便反序列化（注意：这会在 JSON 里带上 class 信息）
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        // --- 核心修改部分结束 ---

        // 使用 Jackson2JsonRedisSerializer 序列化 Value
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(om, Object.class);

        // Key 采用 String 序列化
        template.setKeySerializer(new StringRedisSerializer());
        // Value 采用 JSON 序列化
        template.setValueSerializer(jackson2JsonRedisSerializer);

        // Hash 的 Key/Value 也采用同样的序列化方式
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }
}