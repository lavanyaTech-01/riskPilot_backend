package com.riskpilot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.riskpilot.model.UserCred;
import com.riskpilot.repository.UserRepo;
@Service
public class AuthService {

	@Autowired
	private UserRepo userrepo;
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@Transactional
	public UserCred registerUser(UserCred cred) {
		
		if (userrepo.existsByEmail(cred.getEmail())) {
			throw new IllegalArgumentException("Email already exists");
		}
		 String encodedPassword = passwordEncoder.encode(cred.getPassword());
		 cred.setPassword(encodedPassword);
		
		return userrepo.save(cred);
	}

}
