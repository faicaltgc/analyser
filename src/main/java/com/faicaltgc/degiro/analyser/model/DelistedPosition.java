package com.faicaltgc.degiro.analyser.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "delistedpositions")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"isin"}) // Definiert die Felder für den Vergleich
@Data
public class DelistedPosition {
    @Id
    private String id;
    @Indexed(unique = true)
    private String isin;

    public DelistedPosition(String isin) {
        this.isin = isin;
    } // Constructor for creating a new DelistedPositions object with only the ISIN

}