package com.riskpilot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.riskpilot.model.AiAnalysisResponseDto;
import com.riskpilot.model.DescriptionAnalysisRequest;
import com.riskpilot.model.EmailAnalysisRequest;
import com.riskpilot.model.EmailAnalysisResponse;
import com.riskpilot.model.UrlAnalysisRequest;
import com.riskpilot.model.UrlAnalysisResponse;
import com.riskpilot.model.UserCred;
import com.riskpilot.model.UserPrincipal;
import com.riskpilot.repository.UserRepo;
import com.riskpilot.service.AnalysisService;
import com.riskpilot.service.DescriptionAnalysisService;
import com.riskpilot.service.EmailAnalysisService;
import com.riskpilot.service.HistoryService;
import com.riskpilot.service.UrlAnalysisService;
import com.riskpilot.util.AuthenticationHelper;
import java.util.Optional;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * REST controller for AI analysis and URL scanning endpoints.
 * Provides endpoints to analyze uploaded files and URLs for scam indicators.
 */
@RestController
@RequestMapping("/api/scan")
public class AiAnalysisController {

	@Autowired
	private AnalysisService service;
	
	@Autowired
	private UrlAnalysisService urlAnalysisService;
	
	@Autowired
	private EmailAnalysisService emailAnalysisService;
	
	@Autowired
	private DescriptionAnalysisService descriptionAnalysisService;
	
	@Autowired
	private HistoryService historyService;
	
	@Autowired
	private UserRepo userRepo;
	
	/**
	 * Helper method to safely extract UserCred from Authentication object
	 * Handles both UserPrincipal (JWT) and OAuth2User authentication types
	 */
	private UserCred extractUserCredFromAuthentication(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			System.out.println("⚠️  AiAnalysisController: Authentication is null or not authenticated");
			return null;
		}
		
		Object principal = authentication.getPrincipal();
		
		// Try UserPrincipal first (JWT authenticated users)
		if (principal instanceof UserPrincipal) {
			UserPrincipal userPrincipal = (UserPrincipal) principal;
			UserCred userCred = userPrincipal.getUserCred();
			if (userCred != null) {
				System.out.println("✅ AiAnalysisController: Extracted UserCred from UserPrincipal for: " + userCred.getEmail());
				return userCred;
			}
		}
		
		// Try OAuth2User (OAuth2 authenticated users)
		if (principal instanceof OAuth2User) {
			OAuth2User oAuth2User = (OAuth2User) principal;
			String email = oAuth2User.getAttribute("email");
			System.out.println("⚠️  AiAnalysisController: Found OAuth2User, loading UserCred from database for: " + email);
			Optional<UserCred> userOpt = userRepo.findByEmail(email);
			if (userOpt.isPresent()) {
				UserCred userCred = userOpt.get();
				System.out.println("✅ AiAnalysisController: Loaded UserCred from database for: " + email);
				return userCred;
			}
		}
		
		// Try String (username/email string)
		if (principal instanceof String) {
			String email = (String) principal;
			System.out.println("⚠️  AiAnalysisController: Principal is String, loading UserCred from database for: " + email);
			Optional<UserCred> userOpt = userRepo.findByEmail(email);
			if (userOpt.isPresent()) {
				UserCred userCred = userOpt.get();
				System.out.println("✅ AiAnalysisController: Loaded UserCred from database for: " + email);
				return userCred;
			}
		}
		
		System.out.println("❌ AiAnalysisController: Could not extract UserCred from authentication principal of type: " + principal.getClass().getName());
		return null;
	}
	
	/**
	 * Extract company name from URL domain
	 * Removes common TLDs and returns the company name portion
	 * e.g., "https://amazon.com" -> "Amazon", "https://xyz-careers.com" -> "Xyz-careers"
	 */
	private String extractCompanyNameFromUrl(String url) {
		try {
			if (url == null || url.trim().isEmpty()) {
				return null;
			}
			
			// Remove protocol (http://, https://, www., etc.)
			String domain = url.toLowerCase()
					.replaceAll("^(https?://)?", "")
					.replaceAll("^www\\.", "")
					.split("/")[0]; // Get only domain part
			
			// Remove port if present
			domain = domain.split(":")[0];
			
			// Remove TLD (.com, .co.uk, .org, etc.)
			String[] parts = domain.split("\\.");
			if (parts.length > 0) {
				// Capitalize first letter of domain name
				String companyName = parts[0].replace("-", " ");
				return companyName.substring(0, 1).toUpperCase() + companyName.substring(1);
			}
		} catch (Exception e) {
			System.err.println("Error extracting company name from URL: " + e.getMessage());
			return null;
		}
		return null;
	}
	
	/**
	 * Analyzes an uploaded file (PDF, image, or text) for scam indicators.
	 * Automatically extracts text from the file and performs URL verification.
	 * 
	 * Supported file types:
	 * - PDF files (.pdf)
	 * - Images (.png, .jpg, .jpeg, .tiff, .bmp, .gif) - Uses OCR for text extraction
	 * - Text files (.txt, .csv, .log, .md)
	 * 
	 * @param file The uploaded file to analyze (required)
	 * @return AI analysis response with risk assessment and URL verification results
	 */
	@PostMapping("/file")
	public ResponseEntity<?> scanFile(@RequestParam(required = false) MultipartFile file, Authentication authentication){
		try {
			// Debug logging
			System.out.println("=== File Upload Request Received ===");
			System.out.println("File parameter: " + (file != null ? "Present" : "NULL"));
			if (file != null) {
				System.out.println("File name: " + file.getOriginalFilename());
				System.out.println("File size: " + file.getSize());
				System.out.println("File content-type: " + file.getContentType());
			}
			System.out.println("====================================");
			
			if (file == null || file.isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(new ErrorResponse("File is required. Make sure to upload a file as 'multipart/form-data' with key 'file'", "file_missing"));
			}
			
			AiAnalysisResponseDto response = service.scanFile(file);
			
			// Save response to history if user is authenticated
			if (authentication != null && authentication.isAuthenticated()) {
				try {
					UserCred userCred = extractUserCredFromAuthentication(authentication);
					if (userCred != null) {
						historyService.saveFileAnalysis(file.getOriginalFilename(), file.getContentType(), 
														file.getBytes(), response, userCred);
						System.out.println("✅ File analysis saved to history for user: " + userCred.getEmail());
					} else {
						System.err.println("⚠️  Could not extract UserCred for authenticated user");
					}
				} catch (Exception historyError) {
					// Log the error but don't fail the response
					System.err.println("❌ Failed to save file analysis to history: " + historyError.getMessage());
					historyError.printStackTrace();
				}
			} else {
				System.out.println("⚠️  User is not authenticated or authentication is null");
			}
			
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			String errorMessage = e.getMessage();
			
			// Check if it's a temporary API unavailability error
			if (errorMessage != null && errorMessage.contains("503")) {
				return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
						.body(new ErrorResponse(
							"AI analysis service is temporarily unavailable due to high demand. Please try again in a few moments.",
							"service_temporarily_unavailable"
						));
			}
			
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ErrorResponse("Failed to analyze file: " + errorMessage, "analysis_error"));
		}
	}

	/**
	 * Analyzes a URL for security risks and scam indicators.
	 * Performs 7 verification checks including domain age, SSL validation, 
	 * redirect detection, DNS resolution, malware checking, typosquatting detection,
	 * and URL pattern analysis.
	 * 
	 * Request:
	 * POST /api/scan/url
	 * {
	 *   "url": "https://suspicious-job-link.com",
	 *   "knownCompanyDomain": "amazon.com",      // Optional
	 *   "senderEmail": "recruiter@suspicious.com", // Optional
	 *   "companyName": "Amazon Careers"          // Optional
	 * }
	 * 
	 * Response:
	 * {
	 *   "riskLevel": "HIGH",
	 *   "trustScore": 2.5,
	 *   "domainAgeDays": 12,
	 *   "sslValid": true,
	 *   "redirectDetected": false,
	 *   "malwareFlagged": true,
	 *   "suspiciousIndicators": [...],
	 *   "suggestions": [...],
	 *   "analysisSummary": "..."
	 * }
	 * 
	 * @param request The UrlAnalysisRequest containing the URL to analyze
	 * @return ResponseEntity containing the UrlAnalysisResponse with risk assessment
	 */
	@PostMapping("/url")
	public ResponseEntity<?> analyzeUrl(@RequestBody UrlAnalysisRequest request, Authentication authentication) {
		try {
			// Validate the request
			if (request == null || request.getUrl() == null || request.getUrl().trim().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(new ErrorResponse("URL is required", "url_missing"));
			}

			// Analyze the URL
			UrlAnalysisResponse response = urlAnalysisService.analyzeUrl(request);
			
			// Extract company name from URL
			String companyName = extractCompanyNameFromUrl(request.getUrl());

			// Save response to history if user is authenticated
			if (authentication != null && authentication.isAuthenticated()) {
				try {
					UserCred userCred = extractUserCredFromAuthentication(authentication);
					if (userCred != null) {
						historyService.saveUrlAnalysisWithCompany(request.getUrl(), response, companyName, userCred);
						System.out.println("✅ URL analysis saved to history for user: " + userCred.getEmail() + " with company: " + companyName);
					} else {
						System.err.println("⚠️  Could not extract UserCred for authenticated user");
					}
				} catch (Exception historyError) {
					// Log the error but don't fail the response
					System.err.println("❌ Failed to save URL analysis to history: " + historyError.getMessage());
					historyError.printStackTrace();
				}
			} else {
				System.out.println("⚠️  User is not authenticated or authentication is null");
			}

			return ResponseEntity.ok(response);

		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ErrorResponse(e.getMessage(), "invalid_url"));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ErrorResponse("Failed to analyze URL: " + e.getMessage(), "analysis_error"));
		}
	}

	/**
	 * Analyzes recruiter email address for scam patterns and phishing indicators.
	 * 
	 * Features:
	 * - Domain Trust Classification (TRUSTED, UNKNOWN, SUSPICIOUS)
	 * - Free email provider detection
	 * - Phishing pattern detection with Levenshtein similarity
	 * - Suspicious keyword detection
	 * - Context-aware risk scoring
	 * - Reputation-aware confidence evaluation
	 * 
	 * Request:
	 * POST /api/scan/email
	 * {
	 *   "email": "hr@xyz-careers-job.com"
	 * }
	 * 
	 * Response:
	 * {
	 *   "riskLevel": "HIGH",
	 *   "trustScore": 15,
	 *   "riskScore": 85,
	 *   "email": "hr@xyz-careers-job.com",
	 *   "emailDomain": "xyz-careers-job.com",
	 *   "isFreeEmailProvider": false,
	 *   "domainType": "SUSPICIOUS",
	 *   "isLookalikeDomain": true,
	 *   "phishingSimilarityScore": 82,
	 *   "domainSuspiciousIndicators": ["jobs", "careers"],
	 *   "suspiciousIndicators": [
	 *     "Email uses custom domain with no verified reputation",
	 *     "Phishing pattern detected (score: 82%)",
	 *     "Domain pattern resembles phishing/scam attempts"
	 *   ],
	 *   "suggestions": [
	 *     "⚠ Domain resembles a legitimate company (possible phishing)",
	 *     "Check domain spelling carefully before responding",
	 *     "🚨 HIGH RISK - This appears to be a phishing or scam attempt",
	 *     "❌ Do NOT click links or download attachments",
	 *     "✓ Report this email to your security team"
	 *   ],
	 *   "confidenceLevel": "HIGH"
	 * }
	 * 
	 * @param request The EmailAnalysisRequest containing only the email address
	 * @return ResponseEntity containing the EmailAnalysisResponse with comprehensive risk assessment
	 */
	@PostMapping("/email")
	public ResponseEntity<?> analyzeEmail(@RequestBody EmailAnalysisRequest request, Authentication authentication) {
		try {
			// Validate the request
			if (request == null || request.getEmail() == null || request.getEmail().trim().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(new ErrorResponse("Email is required", "email_missing"));
			}

			// Analyze the email
			EmailAnalysisResponse response = emailAnalysisService.analyzeEmail(request);
			
			// Extract company name from email
			String companyName = EmailAnalysisService.extractCompanyNameFromEmail(request.getEmail());

			// Save response to history if user is authenticated
			if (authentication != null && authentication.isAuthenticated()) {
				try {
					UserCred userCred = extractUserCredFromAuthentication(authentication);
					if (userCred != null) {
						historyService.saveEmailAnalysisWithCompany(request.getEmail(), response, companyName, userCred);
						System.out.println("✅ Email analysis saved to history for user: " + userCred.getEmail() + " with company: " + companyName);
					} else {
						System.err.println("⚠️  Could not extract UserCred for authenticated user");
					}
				} catch (Exception historyError) {
					// Log the error but don't fail the response
					System.err.println("❌ Failed to save email analysis to history: " + historyError.getMessage());
					historyError.printStackTrace();
				}
			} else {
				System.out.println("⚠️  User is not authenticated or authentication is null");
			}

			return ResponseEntity.ok(response);

		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ErrorResponse(e.getMessage(), "invalid_email"));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ErrorResponse("Failed to analyze email: " + e.getMessage(), "analysis_error"));
		}
	}

	/**
	 * Analyzes a job/internship description for scam indicators and risks.
	 * Combines file analysis logic with email and URL verification.
	 * 
	 * Analysis Pipeline:
	 * 1. Extracts emails and URLs from the description
	 * 2. Performs URL verification for any detected URLs
	 * 3. Performs AI-powered scam detection analysis
	 * 4. Returns comprehensive risk assessment with suggestions
	 * 
	 * Request:
	 * POST /api/scan/description
	 * {
	 *   "jobDescription": "<full job/internship description text>"
	 * }
	 * 
	 * Response:
	 * {
	 *   "riskLevel": "HIGH|MEDIUM|LOW",
	 *   "trustScore": 0-10,
	 *   "email": "extracted@email.com",
	 *   "url": "https://suspicious-url.com",
	 *   "emailVerified": false,
	 *   "companyVerified": true,
	 *   "companyDetails": "string",
	 *   "suspiciousIndicators": ["indicator1", "indicator2"],
	 *   "suggestions": ["suggestion1", "suggestion2"],
	 *   "analysisSummary": "string"
	 * }
	 * 
	 * Features:
	 * - Extracts all emails from the description
	 * - Extracts all URLs and verifies them for phishing/malware
	 * - Identifies company name if mentioned
	 * - AI-powered analysis combining textual and URL-based indicators
	 * - Cross-validates URL risk with AI findings
	 * 
	 * @param request The DescriptionAnalysisRequest containing the job description
	 * @return ResponseEntity containing the AiAnalysisResponseDto with comprehensive risk assessment
	 */
	@PostMapping("/description")
	public ResponseEntity<?> analyzeDescription(@RequestBody DescriptionAnalysisRequest request, Authentication authentication) {
		try {
			// Validate the request
			if (request == null || request.getJobDescription() == null || request.getJobDescription().trim().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(new ErrorResponse("Job description is required", "description_missing"));
			}

			// Analyze the description
			AiAnalysisResponseDto response = descriptionAnalysisService.analyzeDescription(request.getJobDescription());

			// Save response to history if user is authenticated
			if (authentication != null && authentication.isAuthenticated()) {
				try {
					UserCred userCred = extractUserCredFromAuthentication(authentication);
					if (userCred != null) {
						historyService.saveDescriptionAnalysis(request.getJobDescription(), response, userCred);
						System.out.println("✅ Description analysis saved to history for user: " + userCred.getEmail());
					} else {
						System.err.println("⚠️  Could not extract UserCred for authenticated user");
					}
				} catch (Exception historyError) {
					// Log the error but don't fail the response
					System.err.println("❌ Failed to save description analysis to history: " + historyError.getMessage());
					historyError.printStackTrace();
				}
			} else {
				System.out.println("⚠️  User is not authenticated or authentication is null");
			}

			return ResponseEntity.ok(response);

		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ErrorResponse(e.getMessage(), "invalid_description"));
		} catch (Exception e) {
			String errorMessage = e.getMessage();
			
			// Check if it's a temporary API unavailability error
			if (errorMessage != null && errorMessage.contains("503")) {
				return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
						.body(new ErrorResponse(
							"AI analysis service is temporarily unavailable due to high demand. Please try again in a few moments.",
							"service_temporarily_unavailable"
						));
			}
			
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ErrorResponse("Failed to analyze description: " + errorMessage, "analysis_error"));
		}
	}

	/**
	 * Helper class for error responses.
	 */
	public static class ErrorResponse {
		private String message;
		private String errorCode;

		public ErrorResponse(String message, String errorCode) {
			this.message = message;
			this.errorCode = errorCode;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public String getErrorCode() {
			return errorCode;
		}

		public void setErrorCode(String errorCode) {
			this.errorCode = errorCode;
		}
	}
}