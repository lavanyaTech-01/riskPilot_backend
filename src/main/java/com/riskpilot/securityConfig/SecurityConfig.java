package com.riskpilot.securityConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.riskpilot.service.CustomOAuth2UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private UserDetailsService userDetails;
	
	@Autowired
	private CustomOAuth2UserService customOAuth2UserService;
	
	@Autowired
	private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
	
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:3001"));
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
		configuration.setAllowedHeaders(Arrays.asList("*"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);
		
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
	
	@Bean
	public AuthenticationProvider authProvider() {  // login purpose

		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetails);

		//provider.setUserDetailsService(userDetails);

		provider.setPasswordEncoder(passwordEncoder());

		return provider;
	}
	
	@Bean
	public SecurityFilterChain filter1(HttpSecurity http, JwtFilter jwtFilter) throws Exception {

		// Enable CORS and disable CSRF, disable HTTP Basic, configure stateless sessions and authentication provider
		http.cors(customizer -> customizer.configurationSource(corsConfigurationSource()))
			.csrf(customizer -> customizer.disable())
			.httpBasic(customizer -> customizer.disable())
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()
				.requestMatchers("/oauth2/**", "/login/**").permitAll()
				.requestMatchers("/api/scan/**").authenticated()
				.requestMatchers("/api/history/**").authenticated()
				.anyRequest().authenticated()
			);

		// Prevents Spring Security from creating an HTTP session
		// Every request is treated independently
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		// Handle authentication exceptions for API requests
		// Return 401 JSON response instead of redirecting to OAuth2 login
		http.exceptionHandling(exception -> exception
			.authenticationEntryPoint((request, response, authException) -> {
				// Check if this is an API request
				String requestURI = request.getRequestURI();
				if (requestURI.startsWith("/api/")) {
					// API request - return 401 JSON response
					response.setContentType("application/json");
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					
					HashMap<String, Object> errorMap = new HashMap<>();
					errorMap.put("success", false);
					errorMap.put("message", "Unauthorized - Invalid or missing JWT token");
					errorMap.put("error", authException.getMessage());
					
					response.getWriter().write(new ObjectMapper().writeValueAsString(errorMap));
					System.out.println("🔒 SecurityConfig: Returning 401 for API request: " + requestURI);
				} else {
					// Non-API request - redirect to OAuth2 login
					response.sendRedirect("/oauth2/authorization/google");
				}
			})
		);

		// Configure OAuth2 login with proper user service and success handler
		http.oauth2Login(oauth2 -> oauth2
			.userInfoEndpoint(userInfo -> userInfo
				.userService(customOAuth2UserService)
			)
			.successHandler(oAuth2AuthenticationSuccessHandler)
			.failureUrl("/login?error=true")
		);

		// Ensure our authentication provider is used
		http.authenticationProvider(authProvider());

		return http.build();
	}
	
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
}