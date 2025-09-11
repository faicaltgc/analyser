package com.faicaltgc.degiro.analyser.service;

import com.faicaltgc.degiro.analyser.dto.TestResponse;
import com.faicaltgc.degiro.analyser.model.Currency;
import com.faicaltgc.degiro.analyser.model.DelistedPosition;
import com.faicaltgc.degiro.analyser.model.Position;
import com.faicaltgc.degiro.analyser.repository.CurrencyRepository;
import com.faicaltgc.degiro.analyser.repository.DelistedPositionRepository;
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
    private DelistedPositionRepository delistedPositionRepository;

    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private CurrencyRepository currencyRepository;


    /**
     * Lädt die Positionen aus der Datenbank und prüft, ob sie veraltet sind.
     *
     * @return
     */
    @Cacheable("positions")
    public List<Position> getPositions() {
        logger.info("Hole Positionen aus der Datenbank (Cache nicht vorhanden oder abgelaufen)");
        List<Position> positions = positionRepository.findAll();
        if (!positions.isEmpty() && isPositionDataOutdated(positions)) {
            //sendPositionToPython(filterPositionsByIsin(positions));
        }
        return positions;
    }

    /**
     * Überprüft, ob die Positionen veraltet sind.
     *
     * @param positions
     * @return
     */
    private boolean isPositionDataOutdated(List<Position> positions) {
        LocalDate today = LocalDate.now();
        return positions.stream().anyMatch(position -> position.getCloseDate() == null || position.getCloseDate().isBefore(today.minusDays(2)));
    }

    /**
     * Verarbeitet die hochgeladene CSV-Datei und erstellt Positionen.
     * @param file
     * @return positionList
     */
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
        //sendPositionToPython(getPositions());
        return getPositions();
    }

    /**
     * Sendet die Positionen an den Python-Service zur Verarbeitung.
     * @param positions
     */
    private void sendPositionToPython(List<Position> positions) {
        try {
            List<DelistedPosition> delistedPositions = delistedPositionRepository.findAll();
            List<Position> filteredPositions = filterDelistedPositions(positions, delistedPositions);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<TestResponse> response = restTemplate.postForEntity("http://localhost:5000/python/data", Map.of("positions", filteredPositions), TestResponse.class);
            if (response.getBody() != null) {
                List<Position> updatedPositions = response.getBody().getUpdatedPositions();
                List<DelistedPosition> delistedISINs = response.getBody().getDelistedISINs();
                float currencyRate = response.getBody().getCurrrencyRate();

                if (updatedPositions != null) {
                    updatePositionPrices(updatedPositions);
                }
                if (delistedISINs != null) {
                    delistedPositions.addAll(delistedISINs);
                    delistedPositionRepository.saveAll(delistedPositions);
                }
                if (currencyRate > 0) {
                    Currency currency = new Currency("USD", currencyRate);
                    currency.setRate(currencyRate);
                    currency.setCloseDate(LocalDate.now());
                    currencyRepository.save(currency);
                }
            }
            logger.info("Python Status Code: {}", response.getStatusCode());
        } catch (Exception e) {
            logger.error("Fehler bei der Kommunikation mit dem Python-Service: {}", e.getMessage(), e);
        }
    }

    /**
     * Filtert die Positionen, um die delisteten Positionen zu entfernen.
     *
     * @param positions
     * @param delistedPositions
     * @return filteredPositions
     */
    private List<Position> filterDelistedPositions(List<Position> positions, List<DelistedPosition> delistedPositions) {
        List<Position> filteredPositions = new ArrayList<>();
        for (Position position : positions) {
            boolean isDelisted = delistedPositions.stream().anyMatch(delisted -> delisted.getIsin().equals(position.getIsin()));
            if (!isDelisted) {
                filteredPositions.add(position);
            }
        }
        return filteredPositions;
    }

    /**
     * Aktualisiert die Preise der Positionen in der Datenbank.
     * @param updatedPositions
     */
    private void updatePositionPrices(List<Position> updatedPositions) {
        Optional<Currency> currencyOptional = currencyRepository.findByCurrencyAndCloseDate("EUR", LocalDate.now());
        for (Position updatedPosition : updatedPositions) {
            Optional<Position> positionOptional = positionRepository.findByIsin(updatedPosition.getIsin());
            positionOptional.ifPresentOrElse(position -> updatePositionIfNecessary(position, updatedPosition, currencyOptional.isPresent() ? currencyOptional.get().getRate() : 1),
                    () -> positionRepository.findByProduct(updatedPosition.getProduct())
                            .ifPresent(position -> updatePositionIfNecessary(position, updatedPosition, currencyOptional.isPresent() ? currencyOptional.get().getRate() : 1)));
        }
    }

    /**
     * Aktualisiert die Position, wenn der Preis größer als 0 ist.
     * Setzt das Close-Datum auf das aktuelle Datum.
     * Speichert die aktualisierte Position in der Datenbank.
     */
    private void updatePositionIfNecessary(Position positionToUpdate, Position updatedPosition, float curencyRate) {
        if (updatedPosition.getClose() > 0) {
            positionToUpdate.setClose(updatedPosition.getClose());
            positionToUpdate.setCloseDate(LocalDate.now());
            if (updatedPosition.getCurrency() != null && !updatedPosition.getCurrency().isEmpty() && updatedPosition.getCurrency().equals("EUR")) {
                positionToUpdate.setSumInEuro(updatedPosition.getSum() * curencyRate);
            }
            positionRepository.save(positionToUpdate);
        }
    }

    /**
     * Aktualisiert den Cache mit den neuen Positionen.
     * Wenn der Cache existiert, werden die neuen Positionen hinzugefügt.
     * Andernfalls wird der Cache mit den neuen Positionen erstellt.
 */
    private void updatePositionCache(List<Position> newPositions) {
        if (!newPositions.isEmpty()) {
            positionRepository.deleteAll();
            positionRepository.insert(newPositions);
            Cache positionsCache = cacheManager.getCache("positions");
            if (positionsCache != null) {
                positionsCache.clear();
                positionsCache.put("positions", newPositions);
            } else {
                logger.warn("Cache 'positions' nicht gefunden. Cache wird nicht aktualisiert.");
                Objects.requireNonNull(cacheManager.getCache("positions")).put("positions", newPositions);
            }


           /* if (positionsCache != null) {
                List<Position> currentPositions = positionsCache.get("positions", List.class);
                if (currentPositions != null) {
                    currentPositions.addAll(newPositions);
                    positionsCache.put("positions", currentPositions);
                } else {
                    positionsCache.put("positions", newPositions);
                }
            }*/
        }
    }

    /**
     * Erstellt eine Position aus einem CSV-Datensatz.
     * @param nextRecord
     * @return
     */
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
            String currency = nextRecord[4].trim().replaceAll("[\\d.,]", "").trim();
            if (!wertMitPunkt.isEmpty()) {
                position.setSum(Float.parseFloat(wertMitPunkt));
            }
            if (!currency.isEmpty()) {
                position.setCurrency(currency);
            }
        }
        if (nextRecord.length > 5 && !nextRecord[5].trim().isEmpty()) {
            position.setSumInEuro(Float.parseFloat(nextRecord[5].trim().replace(",", ".")));
        }
        position.setCloseDate(LocalDate.now());
        return position;
    }

    /**
     * Filtert die Positionen, um nur die mit gültigen ISINs zu behalten.
     *
     * @param positions die Liste der Positionen
     * @return gefilterte Liste der Positionen mit gültigen ISINs
     */
    private List<Position> filterPositionsByIsin(List<Position> positions) {
        List<Position> filteredPositions = new ArrayList<>();
        for (Position position : positions) {
            if (position.getIsin() != null && !position.getIsin().trim().isEmpty()) {
                filteredPositions.add(position);
            }
        }
        return filteredPositions;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public void setPositionRepository(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    public PositionRepository getPositionRepository() {

        return positionRepository;
    }
}