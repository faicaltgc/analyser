package com.faicaltgc.degiro.analyser.repository;

import com.faicaltgc.degiro.analyser.model.Currency;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface CurrencyRepository extends MongoRepository<Currency, String> {
    @Query(value = "{}", fields = "{currency : 1, rate : 1}")
    Optional<Currency> findByCurrencyAndCloseDate(String currency, LocalDate closeDate);
}
