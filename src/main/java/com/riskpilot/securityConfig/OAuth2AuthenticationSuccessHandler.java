package com.riskpilot.securityConfig;

import java.io.IOException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.riskpilot.model.UserCred;
import com.riskpilot.repository.UserRepo;
import com.riskpilot.service.JwtService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles successful OAuth2 authentication and generates JWT tokens
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
	
	@Autowired
	private ObjectProvider<JwtService> jwtServiceProvider;
	
	@Autowired
	private UserRepo userRepo;
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		
		System.out.println("=== OAuth2AuthenticationSuccessHandler called ===");
		
		try {
			// Get OAuth2User from authentication
			OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
			String email = oAuth2User.getAttribute("email");
			
			System.out.println("Email from OAuth2User: " + email);
			
			// Find user in database
			java.util.Optional<UserCred> userOpt = userRepo.findByEmail(email);
			UserCred user = null;
			
			if (!userOpt.isPresent()) {
				System.out.println("User not found in database, creating...");
				// Create user if not exists
				user = new UserCred();
				user.setEmail(email);
				user.setName(((String) oAuth2User.getAttribute("name")).split(" ")[0]);
				user.setFullname((String) oAuth2User.getAttribute("name"));
				user.setPassword("");
				user.setOauth2User(true);
				user = userRepo.save(user);
				System.out.println("User created successfully with ID: " + user.getId());
			} else {
				user = userOpt.get();
				System.out.println("User found in database with ID: " + user.getId());
			}
			
			// Get JwtService from provider to avoid circular dependency
			JwtService jwtService = jwtServiceProvider.getObject();
			
			// Generate JWT tokens
			String accessToken = jwtService.generateTokenForOAuth2User(email);
			String refreshToken = jwtService.generateRefreshTokenForOAuth2User(email);
			
			System.out.println("✅ JWT tokens generated successfully");
			System.out.println("Access Token: " + accessToken.substring(0, 20) + "...");
			System.out.println("Refresh Token: " + refreshToken.substring(0, 20) + "...");
			
			// Redirect to frontend with tokens
			String redirectUrl = "http://localhost:3000/auth/callback?accessToken=" + accessToken + 
									"&refreshToken=" + refreshToken + "&email=" + email;
			
			System.out.println("Redirecting to: " + redirectUrl);
			System.out.println("==============================================");
			
			getRedirectStrategy().sendRedirect(request, response, redirectUrl);
			
		} catch (Exception e) {
			System.out.println("❌ Error in OAuth2AuthenticationSuccessHandler: " + e.getMessage());
			e.printStackTrace();
			super.onAuthenticationSuccess(request, response, authentication);
		}
	}
}
