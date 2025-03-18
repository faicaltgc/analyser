package com.faicaltgc.degiro.analyser.config; // Stellen Sie sicher, dass es im selben config-Package ist

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Date;
import java.time.Instant;
import java.time.ZoneId;

@ReadingConverter
@Component
public class DateToLocalDateConverter implements Converter<Date, LocalDate> {

    @Override
    public LocalDate convert(Date source) {
        if (source == null) {
            return null;
        }
        return Instant.ofEpochMilli(source.getTime())
                .atZone(ZoneId.of("UTC")) // Als UTC interpretieren
                .toLocalDate();
    }
}