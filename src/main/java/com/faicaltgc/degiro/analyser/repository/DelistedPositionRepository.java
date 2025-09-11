package com.faicaltgc.degiro.analyser.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.faicaltgc.degiro.analyser.model.DelistedPosition;

public interface DelistedPositionRepository extends MongoRepository<DelistedPosition, String> {
}
