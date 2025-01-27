package com.faicaltgc.degiro.analyser.dto;

import com.faicaltgc.degiro.analyser.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private User user;

}