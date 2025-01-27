package com.faicaltgc.degiro.analyser.repository;

import com.faicaltgc.degiro.analyser.model.Position;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends MongoRepository<Position,String> {
    Optional<Position> findPositionByIsin(String isin);
    @Query("{isin: ?0}")
    Optional<Position> findByIsin(String isin);
    @Query("{product: ?0}")
    Optional<Position> findByProduct(String product);
    @Query(value="{}", fields="{isin : 1, product :1}")
    List<Position> findIsinAndProduct();
}
