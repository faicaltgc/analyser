package com.faicaltgc.degiro.analyser.config; // Stellen Sie sicher, dass es im config-Package ist

import com.faicaltgc.degiro.analyser.config.DateToLocalDateConverter;
import com.faicaltgc.degiro.analyser.config.LocalDateToDateConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.Arrays;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(Arrays.asList(
                new LocalDateToDateConverter(),
                new DateToLocalDateConverter()
        ));
    }
}