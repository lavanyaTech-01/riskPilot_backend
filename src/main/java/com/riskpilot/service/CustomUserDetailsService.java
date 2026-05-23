 package com.riskpilot.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.riskpilot.model.UserCred;
import com.riskpilot.model.UserPrincipal;
import com.riskpilot.repository.UserRepo;

@Service
public class CustomUserDetailsService implements UserDetailsService{
	
	@Autowired
	private UserRepo userrepo;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		Optional<UserCred> op = userrepo.findByEmail(email);
        if (op.isEmpty()) {
            throw new UsernameNotFoundException("Email not found: " + email);
        }
        UserCred cred = op.get();
        return new UserPrincipal(cred);
		
	}

}
