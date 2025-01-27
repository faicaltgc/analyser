package com.faicaltgc.degiro.analyser.controller;

import com.faicaltgc.degiro.analyser.model.Transaction;
import com.faicaltgc.degiro.analyser.service.TransactionFileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/transaction")
public class TransactionFileIUploadController {
@Autowired
private TransactionFileUploadService transactionFileUploadService;
    @GetMapping("/overview")
    public ResponseEntity<List<Transaction>> getTransactions(){
        try {
            List<Transaction> transactionList = transactionFileUploadService.getTransactions();
            return ResponseEntity.ok(transactionList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    @PostMapping("/upload")
    public ResponseEntity<List<Transaction>> uploadFile(@RequestParam("file") MultipartFile file) {
        System.out.println("transactionfile is :"+file.getName());
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        try {
            List<Transaction> transactionList;
            transactionList = transactionFileUploadService.processCsvFile(file);
            return ResponseEntity.ok(transactionList);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

    }
}
