package com.faicaltgc.degiro.analyser.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;


@Document(collection = "transactions")
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(of = {"orderId", "date", "isin"}) // Definiert die Felder für den Vergleich
public class Transaction {
      @Id
private String id;
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm")
private LocalDateTime date;
private String product;
private String isin;
@Field("typ") // Optional: Wenn du den Feldnamen in der Datenbank explizit steuern willst
private TransactionType typ;
private int number;
private float close;
private String currency;
private float exchangeRate;
private float fee;
private float autoFx;
private float sum;
private String orderId;




}
