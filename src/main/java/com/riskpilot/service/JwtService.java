package com.riskpilot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.riskpilot.model.UserCred;
import com.riskpilot.securityConfig.JwtUtil;


@Service
public class JwtService {
      
	@Autowired
	private AuthenticationManager authManager;
	@Autowired
	private JwtUtil jwtUtil;
	
	public String authenticate(UserCred cred) {
		Authentication authenticate = authManager
				.authenticate(new UsernamePasswordAuthenticationToken(
						cred.getEmail(),
						cred.getPassword()));
		
		if(authenticate.isAuthenticated()) {
			return jwtUtil.generateToken(cred.getEmail());
		} else {
			throw new RuntimeException("Invalid Login Credentials");
		}
		
	}

	public String generateRefreshToken(String email) {
		return jwtUtil.generateRefreshToken(email);
	}

	public String generateTokenForOAuth2User(String email) {
		return jwtUtil.generateToken(email);
	}

	public String generateRefreshTokenForOAuth2User(String email) {
		return jwtUtil.generateRefreshToken(email);
	}

	public String extractToken(String token) {
		return jwtUtil.extractEmail(token);
	}

	public boolean validateToken(String token, UserDetails userDetail) {
		return jwtUtil.validateToken(token, userDetail);
	}

	public String getTokenType(String token) {
		return jwtUtil.getTokenType(token);
	}
	
	public boolean isTokenValid(String token) {
		try {
			jwtUtil.extractEmail(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
}
