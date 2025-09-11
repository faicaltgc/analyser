package com.faicaltgc.degiro.analyser.service;

import com.faicaltgc.degiro.analyser.repository.CurrencyRepository; // Import CurrencyRepository
import com.faicaltgc.degiro.analyser.repository.DelistedPositionRepository; // Import DelistedPositionRepository
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.TestConfiguration; // Import TestConfiguration
import org.springframework.context.annotation.Bean; // Import Bean

import com.faicaltgc.degiro.analyser.model.Position;
import com.faicaltgc.degiro.analyser.repository.PositionRepository;
import com.opencsv.CSVReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class FileProcessingServiceTest {
    @Mock
    private PositionRepository positionRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;
    @Mock // Mock CurrencyRepository as well, if needed by FileProcessingService constructor
    private CurrencyRepository currencyRepository;

    @Mock // Mock DelistedPositionRepository as well, if needed by FileProcessingService constructor
    private DelistedPositionRepository delistedPositionRepository;

    @InjectMocks
    private FileProcessingService fileProcessingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fileProcessingService.getPositions();
    }

    @TestConfiguration // Define a nested Test Configuration class
    static class FileProcessingServiceTestConfiguration { // Give it a descriptive name

        @Bean // Define a Bean of type FileProcessingService that OVERRIDES the default one in the context
        public FileProcessingService fileProcessingService(PositionRepository positionRepository, DelistedPositionRepository delistedPositionRepository, CacheManager cacheManager, CurrencyRepository currencyRepository) {
            // Manually create a FileProcessingService instance, injecting the MOCKS
            FileProcessingService service = new FileProcessingService(); // Verwende den Standardkonstruktor (ohne Argumente)
            service.setCacheManager(cacheManager);
            service.setPositionRepository(positionRepository);
            return service;
        }
    }

    @Test
    void getPositions_shouldReturnPositionsFromCache() {

        // Cache-Konfiguration
        Cache.ValueWrapper valueWrapper = mock(Cache.ValueWrapper.class);
        List<Position> positions = Arrays.asList(new Position(), new Position());

        // Verwenden Sie das Cache-Mock-Objekt aus dem Klassenfeld
        // Mocking the cache manager to return the mock cache
        when(cacheManager.getCache("positions")).thenReturn(cache);
        when(cache.get("positions")).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(positions);
        when(fileProcessingService.getCacheManager().getCache("positions")).thenReturn(cache);
        List<Position> result = fileProcessingService.getPositions();
        assertEquals(positions, result);
        verify(positionRepository, never()).findAll();
    }


    @Test
    void getPositions_shouldReturnPositionsFromDatabaseWhenCacheIsEmpty() {

        List<Position> positions = Arrays.asList(new Position(), new Position());
        when(positionRepository.findAll()).thenReturn(positions);

        List<Position> result = fileProcessingService.getPositions();

        assertEquals(positions, result);
        verify(positionRepository, times(1)).findAll();
    }
/*
    @Test
    void processCsvFile_shouldProcessCsvFileAndReturnPositions() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        Reader reader = new InputStreamReader(getClass().getResourceAsStream("/test.csv"));
        CSVReader csvReader = new CSVReader(reader);
        when(file.getInputStream()).thenReturn(getClass().getResourceAsStream("/test.csv"));
        when(positionRepository.findAll()).thenReturn(Collections.emptyList());

        List<Position> result = fileProcessingService.processCsvFile(file);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
*/

}