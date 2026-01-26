package com.hzwnrw.jellyfin.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Configuration for handling timezone-aware date/time serialization.
 * All ZonedDateTime instances are serialized in ISO-8601 format with timezone info.
 */
@Configuration
public class TimezoneConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(
            ZonedDateTime.class,
            new ZonedDateTimeSerializer(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );
        builder.modules(javaTimeModule);
        
        return builder;
    }
}
