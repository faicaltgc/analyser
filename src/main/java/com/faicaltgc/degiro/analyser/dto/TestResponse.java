package com.faicaltgc.degiro.analyser.dto;

import com.faicaltgc.degiro.analyser.model.DelistedPosition;
import com.faicaltgc.degiro.analyser.model.Position;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestResponse {
    private String message;
    private List<Position> updatedPositions;
    private List<DelistedPosition> delistedISINs;
    private float currrencyRate;
}
