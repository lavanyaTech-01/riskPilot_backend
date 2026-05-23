package com.riskpilot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.riskpilot.model.UserCred;

public interface UserRepo extends JpaRepository<UserCred,Integer>{

	boolean existsByEmail(String email);

	Optional<UserCred> findByEmail(String email);

}
