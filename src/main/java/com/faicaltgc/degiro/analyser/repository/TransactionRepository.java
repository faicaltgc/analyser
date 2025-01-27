package com.faicaltgc.degiro.analyser.repository;

import com.faicaltgc.degiro.analyser.model.Position;
import com.faicaltgc.degiro.analyser.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionRepository extends MongoRepository<Transaction,String> {
    boolean existsByDate(LocalDateTime date);

    @Query(value = "{}", sort = "{ date: 1 }", fields = "{date: 1}")
    Optional<Transaction> findFirstByOrderByDateAsc();

    @Query(value = "{}", sort = "{ date: -1 }", fields = "{date: 1}")
    Optional<Transaction> findFirstByOrderByDateDesc();
}
