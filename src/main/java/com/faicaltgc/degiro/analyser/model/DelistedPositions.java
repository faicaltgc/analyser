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
public class DelistedPositions {
    @Id
    private String id;
    @Indexed(unique = true)
    private String isin;

}
