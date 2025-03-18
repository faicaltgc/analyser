package com.faicaltgc.degiro.analyser.config; // Erstellen Sie ein config-Package

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Date;
import java.time.ZoneOffset;

@WritingConverter
@Component
public class LocalDateToDateConverter implements Converter<LocalDate, Date> {

    @Override
    public Date convert(LocalDate source) {
        if (source == null) {
            return null;
        }
        return Date.from(source.atStartOfDay().toInstant(ZoneOffset.UTC)); // Als UTC speichern, aber nur das Datum
    }
}