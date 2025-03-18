package com.faicaltgc.degiro.analyser.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.faicaltgc.degiro.analyser.model.DelistedPositions;

public interface DelistedPositionsRepository extends MongoRepository<DelistedPositions,String> {
}
