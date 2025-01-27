package com.faicaltgc.degiro.analyser.controller;
import com.faicaltgc.degiro.analyser.model.Position;
import com.faicaltgc.degiro.analyser.service.FileProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class PositionFileUploadController {
    @Autowired
    private FileProcessingService fileProcessingService;
    @GetMapping("/positions")
    public ResponseEntity<List<Position>> getPositions(){
        try {
            List<Position> positionList = fileProcessingService.getPositions();
            return ResponseEntity.ok(positionList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @GetMapping("/Test")
    public ResponseEntity<String> getTest(){
        try {
            return ResponseEntity.ok("hello test");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }




    @PostMapping("/upload")
    public ResponseEntity<List<Position>> uploadFile(@RequestParam("file") MultipartFile file) {
        System.out.println("file is :"+file.getName());
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        try {
            List<Position> positionList = fileProcessingService.processCsvFile(file);
            return ResponseEntity.ok(positionList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

    }
}