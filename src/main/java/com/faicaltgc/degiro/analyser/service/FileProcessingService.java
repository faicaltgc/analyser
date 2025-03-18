package com.faicaltgc.degiro.analyser.service;

import com.faicaltgc.degiro.analyser.dto.TestResponse;
import com.faicaltgc.degiro.analyser.model.DelistedPositions;
import com.faicaltgc.degiro.analyser.model.Position;
import com.faicaltgc.degiro.analyser.repository.DelistedPositionsRepository;
import com.faicaltgc.degiro.analyser.repository.PositionRepository;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.util.*;

@Service
public class FileProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private DelistedPositionsRepository delistedPositionsRepository;

    @Autowired
    private CacheManager cacheManager;


    @Cacheable("positions")
    public List<Position> getPositions() {
        logger.info("Hole Positionen aus der Datenbank (Cache nicht vorhanden oder abgelaufen)");
        List<Position> positions = positionRepository.findAll();
        if (!positions.isEmpty() && isPositionDataOutdated(positions)) {
            sendPositionToPython(filterPositionsByIsin(positions));
        }
        return positions;
    }

    private boolean isPositionDataOutdated(List<Position> positions) {
        LocalDate today = LocalDate.now();
        return positions.stream().anyMatch(position -> position.getCloseDate() == null || position.getCloseDate().isBefore(today.minusDays(2)));
    }

    public List<Position> processCsvFile(MultipartFile file) {
        List<Position> positionList = new ArrayList<>();
        Set<Position> cachedPositions = new HashSet<>(getPositions());

        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {
            csvReader.skip(1); // Überspringt die Header-Zeile
            String[] nextRecord;
            while ((nextRecord = csvReader.readNext()) != null) {
                Position position = createPositionFromCsvRecord(nextRecord);
                if (!cachedPositions.contains(position)) {
                    positionList.add(position);
                }
            }
        } catch (Exception e) {
            logger.error("Fehler beim Verarbeiten der CSV-Datei: {}", e.getMessage(), e);
        }

        updatePositionCache(positionList);
        sendPositionToPython(getPositions());
        return getPositions();
    }

    private void sendPositionToPython(List<Position> positions) {
        try {
            List<DelistedPositions> delistedPositions = delistedPositionsRepository.findAll();
            List<Position> filteredPositions = filterDelistedPositions(positions, delistedPositions);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<TestResponse> response = restTemplate.postForEntity("http://localhost:5000/python/data", Map.of("positions", filteredPositions), TestResponse.class);
            if (response.getBody() != null) {
                List<Position> updatedPositions = response.getBody().getUpdatedPositions();
                List<DelistedPositions> delistedISINs = response.getBody().getDelistedISINs();
                if (updatedPositions != null) {
                    updatePositionPrices(updatedPositions);
                }
                if (delistedISINs != null) {
                    delistedPositions.addAll(delistedISINs);
                    delistedPositionsRepository.saveAll(delistedPositions);
                }
            }
            logger.info("Python Status Code: {}", response.getStatusCode());
        } catch (Exception e) {
            logger.error("Fehler bei der Kommunikation mit dem Python-Service: {}", e.getMessage(), e);
        }
    }

    private List<Position> filterDelistedPositions(List<Position> positions, List<DelistedPositions> delistedPositions) {
        List<Position> filteredPositions = new ArrayList<>();
        for (Position position : positions) {
            boolean isDelisted = delistedPositions.stream().anyMatch(delisted -> delisted.getIsin().equals(position.getIsin()));
            if (!isDelisted) {
                filteredPositions.add(position);
            }
        }
        return filteredPositions;
    }

    private void updatePositionPrices(List<Position> updatedPositions) {
        for (Position updatedPosition : updatedPositions) {
            Optional<Position> positionOptional = positionRepository.findByIsin(updatedPosition.getIsin());
            positionOptional.ifPresentOrElse(position -> updatePositionIfNecessary(position, updatedPosition),
                    () -> positionRepository.findByProduct(updatedPosition.getProduct())
                            .ifPresent(position -> updatePositionIfNecessary(position, updatedPosition)));
        }
    }

    private void updatePositionIfNecessary(Position positionToUpdate, Position updatedPosition) {
        if (updatedPosition.getClose() > 0) {
            positionToUpdate.setClose(updatedPosition.getClose());
            positionToUpdate.setCloseDate(LocalDate.now());
            positionRepository.save(positionToUpdate);
        }
    }

    private void updatePositionCache(List<Position> newPositions) {
        if (!newPositions.isEmpty()) {
            positionRepository.insert(newPositions);
            Cache positionsCache = cacheManager.getCache("positions");
            if (positionsCache != null) {
                List<Position> currentPositions = positionsCache.get("positions", List.class);
                if (currentPositions != null) {
                    currentPositions.addAll(newPositions);
                    positionsCache.put("positions", currentPositions);
                } else {
                    positionsCache.put("positions", newPositions);
                }
            }
        }
    }

    private Position createPositionFromCsvRecord(String[] nextRecord) {
        Position position = new Position();
        if (nextRecord.length > 0) {
            position.setProduct(nextRecord[0].trim());
        }
        if (nextRecord.length > 1) {
            position.setIsin(nextRecord[1].trim());
        }
        if (nextRecord.length > 2 && !nextRecord[2].trim().isEmpty()) {
            position.setNumber(Integer.parseInt(nextRecord[2].trim()));
        }
        if (nextRecord.length > 3 && !nextRecord[3].trim().isEmpty()) {
            position.setClose(Float.parseFloat(nextRecord[3].trim().replace(",", ".")));
        }
        if (nextRecord.length > 4) {
            String wertMitPunkt = nextRecord[4].trim().replace(",", ".").replaceAll("[^\\d.]", "");
            if (!wertMitPunkt.isEmpty()) {
                position.setSum(Float.parseFloat(wertMitPunkt));
            }
        }
        position.setCloseDate(LocalDate.now());
        return position;
    }

    private List<Position> filterPositionsByIsin(List<Position> positions) {
        List<Position> filteredPositions = new ArrayList<>();
        for (Position position : positions) {
            if (position.getIsin() != null && !position.getIsin().trim().isEmpty()) {
                filteredPositions.add(position);
            }
        }
        return filteredPositions;
    }
}