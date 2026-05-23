package com.riskpilot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.riskpilot.model.UserCred;
import com.riskpilot.repository.UserRepo;

/**
 * Custom OAuth2 user service that automatically creates users on first Google login
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
	
	@Autowired
	private UserRepo userRepo;
	
	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) {
		// Load user from Google
		OAuth2User oAuth2User = super.loadUser(userRequest);
		
		// Extract user information from OAuth2User
		String email = oAuth2User.getAttribute("email");
		String name = oAuth2User.getAttribute("name");
		String picture = oAuth2User.getAttribute("picture");
		
		System.out.println("=== OAuth2 User Loaded ===");
		System.out.println("Email: " + email);
		System.out.println("Name: " + name);
		System.out.println("Picture: " + picture);
		System.out.println("==========================");
		
		// Check if user already exists in database
		java.util.Optional<UserCred> existingUserOpt = userRepo.findByEmail(email);
		
		if (!existingUserOpt.isPresent()) {
			// Create new user
			UserCred newUser = new UserCred();
			newUser.setEmail(email);
			newUser.setName(name != null ? name.split(" ")[0] : email.split("@")[0]); // First name
			newUser.setFullname(name != null ? name : email);
			newUser.setPassword(""); // OAuth2 users don't have passwords
			newUser.setOauth2User(true);
			
			UserCred savedUser = userRepo.save(newUser);
			System.out.println("✅ OAuth2 User created successfully with ID: " + savedUser.getId());
		} else {
			// User already exists, just log it
			System.out.println("✅ OAuth2 User already exists with ID: " + existingUserOpt.get().getId());
		}
		
		return oAuth2User;
	}
}
