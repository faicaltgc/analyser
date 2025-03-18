package com.faicaltgc.degiro.analyser.controller;

import com.faicaltgc.degiro.analyser.dto.TestResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

    @RestController
    public class TestController {
        @PostMapping("/python/test")
        public ResponseEntity<TestResponse> getTest(@RequestBody TestResponse testResponse){
            try {
                System.out.println("Data received : "+testResponse.getMessage()+" "+testResponse.getUpdatedPositions());
                return ResponseEntity.ok(testResponse);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(new TestResponse(e.getMessage(),null,null));
            }
        }
    }