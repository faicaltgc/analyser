package com.faicaltgc.degiro.analyser.controller;

import com.faicaltgc.degiro.analyser.dto.AuthRequest;
import com.faicaltgc.degiro.analyser.dto.AuthResponse;
import com.faicaltgc.degiro.analyser.dto.RegisterRequest;
import com.faicaltgc.degiro.analyser.model.User;
import com.faicaltgc.degiro.analyser.repository.UserRepository;
import com.faicaltgc.degiro.analyser.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        userRepository.save(user);
        UserDetails userDetails =  userDetailsService.loadUserByUsername(registerRequest.getUsername());
        String jwtToken = jwtUtil.generateToken(userDetails);
        return new ResponseEntity<>(new AuthResponse(jwtToken,user), HttpStatus.OK);
    }
    private final  org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
        UserDetails userDetails =  userDetailsService.loadUserByUsername(authRequest.getUsername());
        String jwtToken = jwtUtil.generateToken(userDetails);
        User user = userRepository.findByUsername(authRequest.getUsername()).get();
        return new ResponseEntity<>(new AuthResponse(jwtToken,user), HttpStatus.OK);
    }
}