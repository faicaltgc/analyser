package com.faicaltgc.degiro.analyser.service;
import com.faicaltgc.degiro.analyser.dto.TestResponse;
import com.faicaltgc.degiro.analyser.repository.PositionRepository;
import com.faicaltgc.degiro.analyser.model.Position;
import com.opencsv.CSVReader;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;


@Service
public class FileProcessingService {
    @Autowired
    private PositionRepository positionRepository;
    @Autowired
    private CacheManager cacheManager;
    private boolean isUpdatedList = false;

    @Cacheable("positions")
    public List<Position> getPositions() {
        System.out.println("Hole Positionen aus der Datenbank (Cache nicht vorhanden oder abgelaufen)");
        List<Position> positions =  positionRepository.findAll();
        if (positions!=null && !positions.isEmpty()){
            if (isPositionDataOutdated(positions)) {
                sendPositionToPython(filterPositionsByIsin(positions));
            }
        }
        return positions;
    }
    public boolean isPositionDataOutdated(List<Position> positions){
        LocalDate today = LocalDate.now();
        for (Position position : positions) {
            if (position.getCloseDate() == null || position.getCloseDate().isBefore(today.minusDays(2))){
                return true;
            }
        }
        return false;
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
                if (!cachedPositions.contains(position)) { // Vergleich mit dem Cache
                    positionList.add(position);
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        updatePositionCache(positionList); // Cache aktualisieren
        sendPositionToPython(getPositions()); // hier wird der Python Service nach der Speicherung aufgerufen
        return getPositions();
    }


    public void sendPositionToPython(List<Position> positions){
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<TestResponse> response = restTemplate.postForEntity("http://localhost:5000/python/data", Map.of("positions", positions), TestResponse.class);
            if(response.getBody() != null){
                List<Position> updatedPositions = response.getBody().getUpdatedPositions();
                if (updatedPositions!=null){
                    updatePositionPrices(updatedPositions);
                }
            }

            System.out.println("Python Status Code: "+response.getStatusCode());
        } catch (Exception e) {
            System.err.println("Error while communicating with Python Service: "+e.getMessage());
        }
    }


    public void updatePositionPrices(List<Position> updatedPositions) {
        for (Position updatedPosition: updatedPositions){
            Optional<Position> positionOptional =  positionRepository.findByIsin(updatedPosition.getIsin());
            if (positionOptional.isPresent()){
                Position positionToUpdate = positionOptional.get();
                if (updatedPosition.getClose()>0) {
                    positionToUpdate.setClose(updatedPosition.getClose());
                    positionToUpdate.setCloseDate(LocalDate.now());
                }
                positionRepository.save(positionToUpdate);
            } else {
                positionOptional = positionRepository.findByProduct(updatedPosition.getProduct());
                if (positionOptional.isPresent()){
                    Position positionToUpdate = positionOptional.get();
                    if (updatedPosition.getClose()>0){
                        positionToUpdate.setClose(updatedPosition.getClose());
                        positionToUpdate.setCloseDate(LocalDate.now());
                    }
                    positionRepository.save(positionToUpdate);
                }
            }
        }

    }


    private void updatePositionCache(List<Position> newPositions) {
        if (!newPositions.isEmpty()) {
            positionRepository.insert(newPositions);
            Cache positionsCache = cacheManager.getCache("positions");
            if (positionsCache != null) {
                Cache.ValueWrapper valueWrapper = positionsCache.get("positions");
                if (valueWrapper != null) {
                    List<Position> currentPositions = (List<Position>) valueWrapper.get();
                    if (currentPositions != null) {
                        currentPositions.addAll(newPositions);
                        positionsCache.put("positions", currentPositions);
                    } else {
                        positionsCache.put("positions", newPositions);
                    }
                } else {
                    positionsCache.put("positions", newPositions);
                }
            }
        }
    }


    private Position createPositionFromCsvRecord(String[] nextRecord) {
        Position position = new Position();

       // Annahme: Die Reihenfolge der Spalten im CSV ist fix
        // und entspricht: Produkt, Symbol/ISIN, Anzahl, Schlußkurs, Wert
        // Anpassen, falls die Reihenfolge abweicht
        //nextRecord.length > 0): Bevor auf nextRecord[0] zugegriffen wird, wird geprüft,
        // ob das Array überhaupt mindestens ein Element enthält.
        //(nextRecord.length > 1): Bevor auf nextRecord[1] zugegriffen wird, wird geprüft,
        // ob das Array mindestens zwei Elemente enthält.
        //Und so weiter.

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
            // Ersetze Komma durch Punkt für die Umwandlung zu Float
            String wertMitPunkt = nextRecord[4].trim().replace(",", ".");
            // Entferne ggf. Währungssymbole und Tausenderpunkte
            wertMitPunkt = wertMitPunkt.replaceAll("[^\\d.]", "");
            if (!wertMitPunkt.isEmpty()) {
                position.setSum(Float.parseFloat(wertMitPunkt));
            }
        }

        position.setCloseDate(LocalDate.now());

        return position;
    }

    private List<Position> filterPositionsByIsin(List<Position> positions){
        List<Position> filteredPosition=new ArrayList<>(); ;
        for(Position position: positions){
            if(position.getIsin()!=null&&!position.getIsin().trim().isEmpty()){
                filteredPosition.add(position);
            }
        }
        return filteredPosition;
    }

}