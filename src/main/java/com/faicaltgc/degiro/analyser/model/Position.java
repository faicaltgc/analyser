package com.faicaltgc.degiro.analyser.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "positions")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"isin","closeDate"}) // Definiert die Felder für den Vergleich
@Data
public class Position {
    @Id
    private String id;
    @Indexed(unique = true)

    private String isin;
    // Getter und Setter

    private String product;
    private float close;
    private int number;
    private float sum;
    private float buy;
    private float result;
    private LocalDate closeDate;


    @Override
    public String toString() {
        return "PositionData{" +
                "product='" + product + '\'' +
                ", isin='" + isin + '\'' +
                ", number=" + number +
                ", close=" + close +
                ", sum=" + sum +
                '}';
    }



}
