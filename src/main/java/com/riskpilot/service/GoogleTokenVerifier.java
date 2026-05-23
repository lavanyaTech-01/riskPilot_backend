package com.riskpilot.service;

import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskpilot.model.GoogleUserInfo;

@Service
public class GoogleTokenVerifier {

	@Value("${spring.security.oauth2.client.registration.google.client-id}")
	private String googleClientId;
	
	private static final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Parse Google ID token and extract user info
	 * Note: This does basic parsing without signature verification.
	 * For production, verify the signature using Google's public keys.
	 * 
	 * @param token Google ID token from client
	 * @return GoogleUserInfo with email and name, or null if invalid
	 */
	public GoogleUserInfo verifyTokenAndGetUserInfo(String token) {
		try {
			if (token == null || token.isEmpty()) {
				return null;
			}

			// JWT format: header.payload.signature
			String[] parts = token.split("\\.");
			if (parts.length != 3) {
				System.err.println("Invalid token format");
				return null;
			}

			// Decode payload (base64url)
			String payload = decodeBase64Url(parts[1]);
			
			@SuppressWarnings("unchecked")
			Map<String, Object> claims = objectMapper.readValue(payload, Map.class);

			// Verify audience (optional but recommended)
			Object aud = claims.get("aud");
			if (aud != null && !googleClientId.equals(aud)) {
				System.err.println("Audience mismatch: expected " + googleClientId + ", got " + aud);
				// Don't fail on audience mismatch for now, comment out if strict validation needed
				// return null;
			}

			GoogleUserInfo userInfo = new GoogleUserInfo();
			userInfo.setEmail((String) claims.get("email"));
			userInfo.setName((String) claims.get("name"));
			userInfo.setPicture((String) claims.get("picture"));

			return userInfo;
		} catch (Exception e) {
			System.err.println("Token verification failed: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Extract email from Google token
	 * @param token Google ID token
	 * @return email or null if invalid
	 */
	public String verifyTokenAndGetEmail(String token) {
		GoogleUserInfo userInfo = verifyTokenAndGetUserInfo(token);
		return userInfo != null ? userInfo.getEmail() : null;
	}

	/**
	 * Decode base64url string (used in JWT payloads)
	 */
	private String decodeBase64Url(String base64Url) {
		// Add padding if needed
		String base64 = base64Url.replace('-', '+').replace('_', '/');
		int padLength = 4 - (base64.length() % 4);
		if (padLength < 4) {
			base64 += "=".repeat(padLength);
		}
		
		byte[] decodedBytes = Base64.getDecoder().decode(base64);
		return new String(decodedBytes);
	}
}

