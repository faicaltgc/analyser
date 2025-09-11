package com.faicaltgc.degiro.analyser.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;


@Document(collection = "currencies")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"currency,closeDate"}) // Definiert die Felder für den Vergleich
@Data
public class Currency {
    @Id
    private String id;
    @Indexed(unique = true)
    private String currency;
    private float rate;
    private LocalDate closeDate;

    public Currency(String currency, float rate) {
        this.currency = currency;
        this.rate = rate;
    }

}
