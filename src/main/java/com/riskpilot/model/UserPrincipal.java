package com.riskpilot.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;


public class UserPrincipal implements UserDetails, OAuth2User {
	
	private UserCred usercred;
	private Map<String, Object> attributes = new HashMap<>();

	public UserPrincipal(UserCred usercred) {
		this.usercred = usercred;
		// Initialize default OAuth2 attributes
		this.attributes.put("email", usercred.getEmail());
		this.attributes.put("name", usercred.getFullname());
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
	}

	@Override
	public String getPassword() {
		return usercred.getPassword();
	}

	@Override
	public String getUsername() {
		return usercred.getEmail();
	}
	
	public UserCred getUserCred() {
		return usercred;
	}

	// OAuth2User methods
	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public String getName() {
		return usercred.getFullname();
	}
	
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

}
