package com.riskpilot.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.riskpilot.model.UserCred;
import com.riskpilot.model.UserPrincipal;

/**
 * Helper utility class to safely extract UserCred from various authentication types
 * Handles both JWT-authenticated users (UserPrincipal) and OAuth2 users
 */
public class AuthenticationHelper {
	
	/**
	 * Safely extracts UserCred from Authentication object
	 * Handles both UserPrincipal and OAuth2User authentication types
	 * 
	 * @param authentication The Spring Security Authentication object
	 * @return UserCred if found, null otherwise
	 */
	public static UserCred extractUserCred(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			System.out.println("⚠️  AuthenticationHelper: Authentication is null or not authenticated");
			return null;
		}
		
		Object principal = authentication.getPrincipal();
		
		// Handle UserPrincipal (from JWT authentication)
		if (principal instanceof UserPrincipal) {
			UserPrincipal userPrincipal = (UserPrincipal) principal;
			UserCred userCred = userPrincipal.getUserCred();
			if (userCred != null) {
				System.out.println("✅ AuthenticationHelper: Extracted UserCred from UserPrincipal for: " + userCred.getEmail());
				return userCred;
			}
		}
		
		// Handle OAuth2User (direct OAuth2 authentication)
		if (principal instanceof OAuth2User) {
			OAuth2User oAuth2User = (OAuth2User) principal;
			String email = oAuth2User.getAttribute("email");
			System.out.println("⚠️  AuthenticationHelper: Found OAuth2User but UserCred needs to be loaded from database for: " + email);
			// OAuth2User doesn't have direct access to UserCred - it needs to be queried
			return null;
		}
		
		// Handle String (username)
		if (principal instanceof String) {
			System.out.println("⚠️  AuthenticationHelper: Principal is String type, cannot extract UserCred directly");
			return null;
		}
		
		System.out.println("❌ AuthenticationHelper: Unknown principal type: " + principal.getClass().getName());
		return null;
	}
	
	/**
	 * Extracts email from Authentication object
	 * Handles both UserPrincipal and OAuth2User authentication types
	 * 
	 * @param authentication The Spring Security Authentication object
	 * @return Email if found, null otherwise
	 */
	public static String extractEmail(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}
		
		Object principal = authentication.getPrincipal();
		
		// Handle UserPrincipal (from JWT authentication)
		if (principal instanceof UserPrincipal) {
			UserPrincipal userPrincipal = (UserPrincipal) principal;
			UserCred userCred = userPrincipal.getUserCred();
			if (userCred != null) {
				return userCred.getEmail();
			}
		}
		
		// Handle OAuth2User (direct OAuth2 authentication)
		if (principal instanceof OAuth2User) {
			OAuth2User oAuth2User = (OAuth2User) principal;
			return oAuth2User.getAttribute("email");
		}
		
		// Handle String (username)
		if (principal instanceof String) {
			return (String) principal;
		}
		
		return null;
	}
}
