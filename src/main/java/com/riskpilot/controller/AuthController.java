package com.riskpilot.controller;

import java.io.IOException;
import java.util.Map;

import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.riskpilot.model.UserCred;
import com.riskpilot.model.GoogleUserInfo;
import com.riskpilot.service.AuthService;
import com.riskpilot.service.GoogleTokenVerifier;
import com.riskpilot.service.JwtService;

import io.jsonwebtoken.ExpiredJwtException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
	private AuthService authService;
	
	@Autowired
	private JwtService jwtService;

	@Autowired
	private GoogleTokenVerifier googleTokenVerifier;

	@PostMapping("/signup")
	public ResponseEntity<?> signUp(@RequestBody UserCred cred) {
		try {
			// Check if it's a Google OAuth signup
			if (cred.getGoogleToken() != null && !cred.getGoogleToken().isEmpty()) {
				GoogleUserInfo userInfo = googleTokenVerifier.verifyTokenAndGetUserInfo(cred.getGoogleToken());
				
				if (userInfo == null) {
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
							.body(Map.of("message", "Invalid Google token"));
				}
				
				// Set user info from Google token
				cred.setEmail(userInfo.getEmail());
				if (cred.getName() == null || cred.getName().isEmpty()) {
					cred.setName(userInfo.getName());
				}
				cred.setPassword(null); // No password for OAuth users
			}
			
			UserCred savedCred = authService.registerUser(cred);
			return new ResponseEntity<>(savedCred, HttpStatus.CREATED);
		} catch (IllegalArgumentException ie) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ie.getMessage()));
		}
	}
	
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody UserCred cred){
		// Check if it's a Google OAuth login
		if (cred.getGoogleToken() != null && !cred.getGoogleToken().isEmpty()) {
			GoogleUserInfo userInfo = googleTokenVerifier.verifyTokenAndGetUserInfo(cred.getGoogleToken());
			
			if (userInfo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("message", "Invalid Google token"));
			}
			
			// Generate both JWT access and refresh tokens for OAuth user
			String accessToken = jwtService.generateTokenForOAuth2User(userInfo.getEmail());
			String refreshToken = jwtService.generateRefreshTokenForOAuth2User(userInfo.getEmail());
			
			return ResponseEntity.ok(Map.of(
				"message", "OAuth Login successful!",
				"accessToken", accessToken,
				"refreshToken", refreshToken,
				"user", Map.of(
					"email", userInfo.getEmail(),
					"name", userInfo.getName()
				)
			));
		}
		
		// Traditional email/password login
		if (cred.getEmail() == null || cred.getPassword() == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Username and password required"));
		}

		// Attempt authentication and return both JWT tokens; if invalid, return 401 with status/message
		try {
			String accessToken = jwtService.authenticate(cred);
			String refreshToken = jwtService.generateRefreshToken(cred.getEmail());
			
			return ResponseEntity.ok(Map.of(
				"message", "Login successful!", 
				"accessToken", accessToken,
				"refreshToken", refreshToken
			));
		} catch (Exception ex) {
			// Authentication failed (bad credentials)
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("status", "error", "message", "Invalid email and password"));
		}
	}

	@PostMapping("/refresh")
	public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
		try {
			String refreshToken = request.get("refreshToken");
			
			if (refreshToken == null || refreshToken.isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(Map.of("message", "Refresh token is required"));
			}
			
			try {
				// Verify that it's actually a refresh token
				String tokenType = jwtService.getTokenType(refreshToken);
				if (tokenType == null || !tokenType.equals("REFRESH")) {
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
							.body(Map.of("message", "Invalid refresh token"));
				}
				
				// Extract email and generate new access token
				String email = jwtService.extractToken(refreshToken);
				String newAccessToken = jwtService.generateTokenForOAuth2User(email);
				
				return ResponseEntity.ok(Map.of(
					"accessToken", newAccessToken,
					"message", "Token refreshed successfully!"
				));
			} catch (ExpiredJwtException e) {
				// Refresh token itself has expired
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("status", "error", "message", "Refresh token has expired. Please login again."));
			}
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("status", "error", "message", "Invalid refresh token: " + ex.getMessage()));
		}
	}

}
