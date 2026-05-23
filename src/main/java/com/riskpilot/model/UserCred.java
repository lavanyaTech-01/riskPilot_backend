package com.riskpilot.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCred {
	@Id
	@GeneratedValue
	private Long id;

	private String name;
	private String fullname;  // Full name of user
	private String email;
	private String password;
	
	@Column(name = "oauth2_user", nullable = true)
	private boolean oauth2User = false;  // Flag to indicate if user came from OAuth2

	@jakarta.persistence.Transient
	private String googleToken;

	@OneToMany(mappedBy = "user")
	@JsonIgnore
	private List<HistoryReport> reports;
}
