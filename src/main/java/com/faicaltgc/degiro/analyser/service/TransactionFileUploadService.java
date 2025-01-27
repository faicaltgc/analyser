package com.faicaltgc.degiro.analyser.service;

import com.faicaltgc.degiro.analyser.model.Transaction;
import com.faicaltgc.degiro.analyser.model.TransactionType;
import com.faicaltgc.degiro.analyser.repository.TransactionRepository;
import com.opencsv.CSVReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TransactionFileUploadService {
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private CacheManager cacheManager;
    private final float AUTOFX = 0.0025F;
    private final String EURO = "EUR";
    private boolean isUpdatedList = false;

    @Cacheable("transactions")
    public List<Transaction> getTransactions() {
        System.out.println("Hole Transaktionen aus der Datenbank (Cache nicht vorhanden oder abgelaufen)");
        return transactionRepository.findAll();
    }

    public List<Transaction> processCsvFile(MultipartFile file) {
        List<Transaction> transactionList = new ArrayList<>();
        Set<Transaction> cachedTransactions = new HashSet<>(getTransactions());
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {
            csvReader.skip(1); // Überspringt die Header-Zeile
            String[] nextRecord;
            while ((nextRecord = csvReader.readNext()) != null) {
                Transaction transaction = createTransactionFromCsvRecord(nextRecord);
                if (!cachedTransactions.contains(transaction)&&transaction.getDate()!=null) { // Vergleich mit dem Cache
                    transactionList.add(transaction);
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        updateTransactionCache(transactionList); // Cache aktualisieren
        return getTransactions();
    }

    public static LocalDateTime kombiniereDatumUndZeit(String datumString, String zeitString) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        try {
            LocalDate datum = LocalDate.parse(datumString, dateFormatter);
            LocalTime zeit = LocalTime.parse(zeitString, timeFormatter);
            return LocalDateTime.of(datum, zeit);
        } catch (DateTimeParseException e) {
            System.err.println("Fehler beim Parsen von Datum oder Zeit: " + e.getMessage());
            return null;
        }
    }

    private void updateTransactionCache(List<Transaction> newTransactions) {
        if (!newTransactions.isEmpty()) {
            transactionRepository.insert(newTransactions);
            Cache transactionsCache = cacheManager.getCache("transactions");
            if (transactionsCache != null) {
                Cache.ValueWrapper valueWrapper = transactionsCache.get("transactions");
                if (valueWrapper != null) {
                    List<Transaction> currentTransactions = (List<Transaction>) valueWrapper.get();
                    if (currentTransactions != null) {
                        currentTransactions.addAll(newTransactions);
                        transactionsCache.put("transactions", currentTransactions);
                    } else {
                        transactionsCache.put("transactions", newTransactions);
                    }
                } else {
                    transactionsCache.put("transactions", newTransactions);
                }
            }
        }
    }

    private Transaction createTransactionFromCsvRecord(String[] nextRecord) {
        Transaction transaction = new Transaction();

        // Annahme: Die Reihenfolge der Spalten im CSV ist fix
        // und entspricht: Produkt, Symbol/ISIN, Anzahl, Schlußkurs, Wert
        // Anpassen, falls die Reihenfolge abweicht
        //nextRecord.length > 0): Bevor auf nextRecord[0] zugegriffen wird, wird geprüft,
        // ob das Array überhaupt mindestens ein Element enthält.
        //(nextRecord.length > 1): Bevor auf nextRecord[1] zugegriffen wird, wird geprüft,
        // ob das Array mindestens zwei Elemente enthält.
        //Und so weiter.
        String datumsSpalte = ""; // Annahme: Datumsspalte ist die erste
        String zeitSpalte = "";
        float sum = 0;
        if (nextRecord.length > 0 && !nextRecord[0].trim().isEmpty()) {
            datumsSpalte = nextRecord[0].trim();
        }
        if (nextRecord.length > 1 && !nextRecord[1].trim().isEmpty()) {
            zeitSpalte = nextRecord[1].trim();
            // Datum und Zeit zusammenführen und parsen
            LocalDateTime kombiniertesDatumZeit = kombiniereDatumUndZeit(datumsSpalte, zeitSpalte);
            transaction.setDate(kombiniertesDatumZeit);
        }
        if (nextRecord.length > 2 && !nextRecord[2].trim().isEmpty()) {
            transaction.setProduct(nextRecord[2].trim());
        }
        if (nextRecord.length > 3 && !nextRecord[3].trim().isEmpty()) {
            transaction.setIsin(nextRecord[3].trim());
        }
        if (nextRecord.length > 6 && !nextRecord[6].trim().isEmpty()) {
            transaction.setNumber(Integer.parseInt(nextRecord[6].trim()));
        }
        if (nextRecord.length > 7 && !nextRecord[7].trim().isEmpty()) {
            transaction.setClose(Float.parseFloat(nextRecord[7].trim().replace(",", ".")));
        }
        if (nextRecord.length > 8 && !nextRecord[8].trim().isEmpty()) {
            transaction.setCurrency(nextRecord[8].trim());
        }
        if (nextRecord.length > 11 && !nextRecord[11].trim().isEmpty()) {
            sum = Float.parseFloat(nextRecord[11].trim().replace(",", "."));

        }
        if (nextRecord.length > 13 && !nextRecord[13].trim().isEmpty()) {
            transaction.setExchangeRate(Float.parseFloat(nextRecord[13].trim().replace(",", ".")));
        }
        if (nextRecord.length > 14 && !nextRecord[14].trim().isEmpty()) {
            transaction.setFee(Float.parseFloat(nextRecord[14].trim().replace(",", ".")));
            if (!transaction.getCurrency().equals(EURO) && transaction.getFee() < 0) {
                transaction.setAutoFx(sum * AUTOFX);
            }
        }
        if (nextRecord.length > 16 && !nextRecord[16].trim().isEmpty()) {
            transaction.setSum(Float.parseFloat(nextRecord[16].trim().replace(",", ".")) - transaction.getAutoFx());
        }

        if (nextRecord.length > 18) {
            if (!nextRecord[18].trim().isEmpty()){
                transaction.setOrderId(nextRecord[18].trim());
            }
            if (nextRecord[18].trim().isEmpty() && transaction.getFee() == 0) {
                transaction.setTyp(TransactionType.SPLIT);
            } else if (!nextRecord[18].trim().isEmpty() && transaction.getFee() < 0 && transaction.getNumber() > 0) {
                transaction.setTyp(TransactionType.KAUF);
            } else {
                transaction.setTyp(TransactionType.VERKAUF);
            }
        }
        return transaction; // Placeholder
    }



}
