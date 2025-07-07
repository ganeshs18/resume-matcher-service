package com.example.resumeMatcherService.controller;


import com.example.resumeMatcherService.dto.CommonResponse;
import com.example.resumeMatcherService.dto.request.UserLoginRequest;
import com.example.resumeMatcherService.dto.request.UserSignupRequest;
import com.example.resumeMatcherService.dto.response.UserLoginResponseDTO;
import com.example.resumeMatcherService.service.AuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private AuthService authService;

    @Value("${google.client.id}")
    private String googleClientId; // Replace with your actual Google Client ID

    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<UserLoginResponseDTO>> register(@RequestBody UserSignupRequest request) {
        UserLoginResponseDTO result = authService.signup(request);
        CommonResponse<UserLoginResponseDTO> response = new CommonResponse<>("Signup successful", result, HttpStatus.OK);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/google")
    public ResponseEntity<?> authenticateWithGoogle(@RequestBody Map<String, String> body) {
        String idTokenString = body.get("token");

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                String email = payload.getEmail();
                String name = (String) payload.get("name");

                // Optionally: Check if user exists in DB, else register
                UserLoginResponseDTO userResponse = authService.loginOrRegisterGoogleUser(email, name);
                // Return JWT from your system, or session data
                return ResponseEntity.ok(userResponse);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Token verification failed: " + e.getMessage());
        }
    }
}
