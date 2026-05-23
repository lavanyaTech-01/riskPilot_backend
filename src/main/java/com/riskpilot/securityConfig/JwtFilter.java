package com.riskpilot.securityConfig;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.riskpilot.model.UserCred;
import com.riskpilot.repository.UserRepo;
import com.riskpilot.service.CustomUserDetailsService;
import com.riskpilot.service.JwtService;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter{
	
	@Autowired
	private JwtService jwtService;
	
	@Autowired
	private UserRepo userRepo;
	
	@Autowired
	ApplicationContext context;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
	
		// Extract Authorization header FIRST (before checking multipart)
		// This ensures JWT is validated even for multipart requests (file uploads)
		String authHeader = request.getHeader("Authorization");
		String token = null;
		String email = null;

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			token = authHeader.substring(7);
			try {
				email = jwtService.extractToken(token);
				System.out.println("🔍 JwtFilter: Extracted email from JWT token: " + email);
			} catch (ExpiredJwtException e) {
				// Token is expired, allow the request to proceed without authentication
				// The endpoint will return a proper error response or client can use refresh token
				System.out.println("JWT token is expired, allowing request to proceed without authentication");
				filterChain.doFilter(request, response);
				return;
			} catch (Exception e) {
				// Invalid token, allow the request to proceed
				System.out.println("Invalid JWT token: " + e.getMessage());
				filterChain.doFilter(request, response);
				return;
			}
		}
		
		// Skip body consumption for multipart requests
		// But JWT authentication has already been extracted above
		String contentType = request.getContentType();
		if (contentType != null && contentType.contains("multipart/form-data")) {
			System.out.println("📁 JwtFilter: Multipart request detected, will not consume body");
		}
		
		// Authenticate the user if we have a valid email from JWT
		if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			try {
				// First check if user exists in database
				UserDetails userDetail = context.getBean(CustomUserDetailsService.class).loadUserByUsername(email);

				if (jwtService.validateToken(token, userDetail)) {
					System.out.println("✅ JWT validated for existing user: " + email);
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetail,
							null, userDetail.getAuthorities());
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
					System.out.println("✅ JwtFilter: Set UserPrincipal in security context for: " + email);
				}
			} catch (Exception e) {
				// User not found - auto-create the user for Google OAuth
				System.out.println("⚠️  User not found in database: " + email + ", attempting auto-creation...");
				try {
					if (jwtService.isTokenValid(token)) {
						// Token is valid but user doesn't exist, create the user
						UserCred newUser = new UserCred();
						newUser.setEmail(email);
						newUser.setName(email.split("@")[0]); // Use part before @ as name
						newUser.setPassword(""); // OAuth users don't have passwords
						newUser.setOauth2User(true);
						newUser.setFullname(email);
						
						UserCred savedUser = userRepo.save(newUser);
						System.out.println("✅ Auto-created Google OAuth user: " + email + " with ID: " + savedUser.getId());
						
						// Now authenticate the user - this will create UserPrincipal
						UserDetails userDetail = context.getBean(CustomUserDetailsService.class).loadUserByUsername(email);
						UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetail,
								null, userDetail.getAuthorities());
						authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(authToken);
						System.out.println("✅ JwtFilter: Successfully authenticated auto-created OAuth2 user: " + email);
					}
				} catch (Exception ex) {
					System.err.println("❌ JwtFilter: Failed to auto-create user: " + email);
					ex.printStackTrace();
				}
			}
		}
		filterChain.doFilter(request, response);
		
	}

}