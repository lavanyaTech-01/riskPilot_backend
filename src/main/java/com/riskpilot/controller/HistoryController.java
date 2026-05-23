package com.riskpilot.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.riskpilot.model.AiAnalysisResponseDto;
import com.riskpilot.model.EmailAnalysisResponse;
import com.riskpilot.model.HistoryReport;
import com.riskpilot.model.UrlAnalysisResponse;
import com.riskpilot.model.UserCred;
import com.riskpilot.model.UserPrincipal;
import com.riskpilot.repository.UserRepo;
import com.riskpilot.service.AnalysisService;
import com.riskpilot.service.HistoryService;

/**
 * REST controller for scan history management
 * Provides endpoints to retrieve, filter, and manage scan records
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

	@Autowired
	private HistoryService historyService;
	
	@Autowired
	private AnalysisService analysisService;
	
	@Autowired
	private UserRepo userRepo;
	
	/**
	 * Helper method to safely extract UserCred from Authentication object
	 * Handles both UserPrincipal (JWT) and OAuth2User authentication types
	 */
	private UserCred extractUserCredFromAuthentication(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			System.out.println("⚠️  HistoryController: Authentication is null or not authenticated");
			return null;
		}
		
		Object principal = authentication.getPrincipal();
		
		// Try UserPrincipal first (JWT authenticated users)
		if (principal instanceof UserPrincipal) {
			UserPrincipal userPrincipal = (UserPrincipal) principal;
			UserCred userCred = userPrincipal.getUserCred();
			if (userCred != null) {
				System.out.println("✅ HistoryController: Extracted UserCred from UserPrincipal for: " + userCred.getEmail());
				return userCred;
			}
		}
		
		// Try OAuth2User (OAuth2 authenticated users)
		if (principal instanceof OAuth2User) {
			OAuth2User oAuth2User = (OAuth2User) principal;
			String email = oAuth2User.getAttribute("email");
			System.out.println("⚠️  HistoryController: Found OAuth2User, loading UserCred from database for: " + email);
			Optional<UserCred> userOpt = userRepo.findByEmail(email);
			if (userOpt.isPresent()) {
				UserCred userCred = userOpt.get();
				System.out.println("✅ HistoryController: Loaded UserCred from database for: " + email);
				return userCred;
			}
		}
		
		// Try String (username/email string)
		if (principal instanceof String) {
			String email = (String) principal;
			System.out.println("⚠️  HistoryController: Principal is String, loading UserCred from database for: " + email);
			Optional<UserCred> userOpt = userRepo.findByEmail(email);
			if (userOpt.isPresent()) {
				UserCred userCred = userOpt.get();
				System.out.println("✅ HistoryController: Loaded UserCred from database for: " + email);
				return userCred;
			}
		}
		
		System.out.println("❌ HistoryController: Could not extract UserCred from authentication principal of type: " + principal.getClass().getName());
		return null;
	}

	/**
	 * Get all scan history for the authenticated user
	 * Returns scans ordered by creation date (newest first)
	 */
	@GetMapping("/all")
	public ResponseEntity<?> getAllHistory(Authentication authentication) {
		try {
			UserCred user = extractUserCredFromAuthentication(authentication);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("success", false, "error", "Could not authenticate user"));
			}

			List<HistoryReport> history = historyService.getUserHistory(user);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("totalRecords", history.size());
			response.put("data", history);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "error", "Failed to retrieve history: " + e.getMessage()));
		}
	}

	/**
	 * Get a single scan record by ID with all details including deserialized responses
	 * Displays complete information about the scan
	 */
	@GetMapping("/{scanId}")
	public ResponseEntity<?> getScanById(@PathVariable Long scanId, Authentication authentication) {
		try {
			HistoryReport scan = historyService.getScanById(scanId);

			if (scan == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(Map.of("success", false, "error", "Scan record not found"));
			}

			// Verify the scan belongs to the authenticated user
			UserCred user = extractUserCredFromAuthentication(authentication);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("success", false, "error", "Could not authenticate user"));
			}

			if (!scan.getUser().getId().equals(user.getId())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("success", false, "error", "Access denied to this scan record"));
			}

			// Build detailed response with all scan information and deserialized responses
			Map<String, Object> detailedResponse = buildDetailedScanResponse(scan);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("data", detailedResponse);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "error", "Failed to retrieve scan: " + e.getMessage()));
		}
	}

	/**
	 * Build a comprehensive response with all scan details and deserialized responses
	 */
	private Map<String, Object> buildDetailedScanResponse(HistoryReport scan) {
		Map<String, Object> detailed = new HashMap<>();

		// Basic scan information
		Map<String, Object> scanInfo = new HashMap<>();
		scanInfo.put("id", scan.getId());
		scanInfo.put("scanType", scan.getScanType());
		scanInfo.put("createdAt", scan.getCreatedAt());
		scanInfo.put("updatedAt", scan.getUpdatedAt());
		detailed.put("scanInfo", scanInfo);

		// Scan inputs
		Map<String, Object> inputs = new HashMap<>();
		if (scan.getUrl() != null) {
			inputs.put("url", scan.getUrl());
		}
		if (scan.getEmail() != null) {
			inputs.put("email", scan.getEmail());
		}
		if (scan.getFileName() != null) {
			inputs.put("fileName", scan.getFileName());
		}
		if (scan.getFileType() != null) {
			inputs.put("fileType", scan.getFileType());
		}
		if (scan.getJobDescription() != null) {
			inputs.put("jobDescription", scan.getJobDescription());
		}
		if (scan.getCompanyName() != null) {
			inputs.put("companyName", scan.getCompanyName());
		}
		detailed.put("inputs", inputs);

		// Analysis results summary
		Map<String, Object> resultsSummary = new HashMap<>();
		resultsSummary.put("riskLevel", scan.getRiskLevel());
		resultsSummary.put("trustScore", scan.getTrustScore());
		resultsSummary.put("riskScore", scan.getRiskScore());
		resultsSummary.put("emailVerified", scan.getEmailVerified());
		resultsSummary.put("companyVerified", scan.getCompanyVerified());
		resultsSummary.put("companyDetails", scan.getCompanyDetails());
		resultsSummary.put("analysisSummary", scan.getAnalysisSummary());
		resultsSummary.put("suspiciousIndicators", scan.getSuspiciousIndicators());
		resultsSummary.put("suggestions", scan.getSuggestions());
		detailed.put("resultsSummary", resultsSummary);

		// Detailed responses (deserialized from JSON)
		Map<String, Object> detailedResponses = new HashMap<>();

		// URL Analysis Response
		if (scan.getUrlAnalysisResponse() != null) {
			try {
				UrlAnalysisResponse urlResponse = historyService.getUrlAnalysisResponse(scan);
				detailedResponses.put("urlAnalysis", urlResponse);
			} catch (Exception e) {
				detailedResponses.put("urlAnalysis", Map.of("error", "Failed to deserialize: " + e.getMessage()));
			}
		}

		// Email Analysis Response
		if (scan.getEmailAnalysisResponse() != null) {
			try {
				EmailAnalysisResponse emailResponse = historyService.getEmailAnalysisResponse(scan);
				detailedResponses.put("emailAnalysis", emailResponse);
			} catch (Exception e) {
				detailedResponses.put("emailAnalysis", Map.of("error", "Failed to deserialize: " + e.getMessage()));
			}
		}

		// AI Analysis Response (for FILE and DESCRIPTION scans)
		if (scan.getAiAnalysisResponse() != null) {
			try {
				AiAnalysisResponseDto aiResponse = historyService.getAiAnalysisResponse(scan);
				detailedResponses.put("aiAnalysis", aiResponse);
			} catch (Exception e) {
				detailedResponses.put("aiAnalysis", Map.of("error", "Failed to deserialize: " + e.getMessage()));
			}
		}

		detailed.put("detailedResponses", detailedResponses);

		return detailed;
	}

	/**
	 * Get scan history by type (URL, EMAIL, FILE, DESCRIPTION)
	 */
	@GetMapping("/type/{scanType}")
	public ResponseEntity<?> getHistoryByType(@PathVariable String scanType, Authentication authentication) {
		try {
			UserCred user = extractUserCredFromAuthentication(authentication);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("success", false, "error", "Could not authenticate user"));
			}

			List<HistoryReport> history = historyService.getUserHistoryByType(user, scanType.toUpperCase());

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("scanType", scanType.toUpperCase());
			response.put("totalRecords", history.size());
			response.put("data", history);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "error", "Failed to retrieve history by type: " + e.getMessage()));
		}
	}

	/**
	 * Get HIGH risk scans for the user
	 */
	@GetMapping("/high-risk")
	public ResponseEntity<?> getHighRiskScans(Authentication authentication) {
		try {
			UserCred user = extractUserCredFromAuthentication(authentication);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("success", false, "error", "Could not authenticate user"));
			}

			List<HistoryReport> highRiskScans = historyService.getHighRiskScans(user);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("totalHighRiskRecords", highRiskScans.size());
			response.put("data", highRiskScans);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "error", "Failed to retrieve high risk scans: " + e.getMessage()));
		}
	}

	/**
	 * Get scan history by risk level (LOW, MEDIUM, HIGH)
	 */
	@GetMapping("/risk-level/{riskLevel}")
	public ResponseEntity<?> getHistoryByRiskLevel(@PathVariable String riskLevel, Authentication authentication) {
		try {
			UserCred user = extractUserCredFromAuthentication(authentication);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("success", false, "error", "Could not authenticate user"));
			}

			List<HistoryReport> history = historyService.getUserHistoryByRiskLevel(user, riskLevel.toUpperCase());

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("riskLevel", riskLevel.toUpperCase());
			response.put("totalRecords", history.size());
			response.put("data", history);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "error", "Failed to retrieve history by risk level: " + e.getMessage()));
		}
	}

	/**
	 * Get scan history within a date range
	 * Query params: startDate, endDate (format: yyyy-MM-dd'T'HH:mm:ss)
	 */
	@GetMapping("/date-range")
	public ResponseEntity<?> getHistoryByDateRange(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
			Authentication authentication) {
		try {
			UserCred user = extractUserCredFromAuthentication(authentication);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("success", false, "error", "Could not authenticate user"));
			}

			List<HistoryReport> history = historyService.getScansByDateRange(user, startDate, endDate);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("startDate", startDate);
			response.put("endDate", endDate);
			response.put("totalRecords", history.size());
			response.put("data", history);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "error", "Failed to retrieve history by date range: " + e.getMessage()));
		}
	}

	/**
	 * Delete a single scan record by ID
	 */
	@DeleteMapping("/delete/{scanId}")
	public ResponseEntity<?> deleteScan(@PathVariable Long scanId, Authentication authentication) {
		try {
			HistoryReport scan = historyService.getScanById(scanId);

			if (scan == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(Map.of("success", false, "error", "Scan record not found"));
			}

			// Verify the scan belongs to the authenticated user
			UserCred user = extractUserCredFromAuthentication(authentication);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("success", false, "error", "Could not authenticate user"));
			}

			if (!scan.getUser().getId().equals(user.getId())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("success", false, "error", "Access denied to delete this scan record"));
			}

			historyService.deleteScan(scanId);

			return ResponseEntity.ok(Map.of("success", true, "message", "Scan record deleted successfully"));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "error", "Failed to delete scan: " + e.getMessage()));
		}
	}

	/**
	 * Get statistics for the user's scan history
	 */
	@GetMapping("/statistics/summary")
	public ResponseEntity<?> getHistoryStatistics(Authentication authentication) {
		try {
			UserCred user = extractUserCredFromAuthentication(authentication);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("success", false, "error", "Could not authenticate user"));
			}

			long totalScans = historyService.getTotalScansCount(user);
			long urlScans = historyService.getScanCountByType(user, "URL");
			long emailScans = historyService.getScanCountByType(user, "EMAIL");
			long fileScans = historyService.getScanCountByType(user, "FILE");
			long descriptionScans = historyService.getScanCountByType(user, "DESCRIPTION");

			List<HistoryReport> highRiskScans = historyService.getHighRiskScans(user);
			long highRiskCount = highRiskScans.size();

			Map<String, Object> statistics = new HashMap<>();
			statistics.put("totalScans", totalScans);
			statistics.put("urlScans", urlScans);
			statistics.put("emailScans", emailScans);
			statistics.put("fileScans", fileScans);
			statistics.put("descriptionScans", descriptionScans);
			statistics.put("highRiskScans", highRiskCount);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("statistics", statistics);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "error", "Failed to retrieve statistics: " + e.getMessage()));
		}
	}

	/**
	 * Save URL analysis response to history
	 * User clicks "Save to History" button after scanning a URL
	 */
	@PostMapping("/save/url")
	public ResponseEntity<?> saveUrlToHistory(@RequestBody Map<String, Object> request, Authentication authentication) {
		try {
			UserCred user = extractUserCredFromAuthentication(authentication);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("success", false, "error", "Could not authenticate user"));
			}

			String url = (String) request.get("url");
			@SuppressWarnings("unchecked")
			Map<String, Object> responseData = (Map<String, Object>) request.get("response");

			if (url == null || url.trim().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(Map.of("success", false, "error", "URL is required"));
			}

			if (responseData == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(Map.of("success", false, "error", "Analysis response is required"));
			}

			// Convert map to UrlAnalysisResponse
			UrlAnalysisResponse response = new com.fasterxml.jackson.databind.ObjectMapper()
					.convertValue(responseData, UrlAnalysisResponse.class);

			// Save to history
			HistoryReport history = historyService.saveUrlAnalysis(url, response, user);

			Map<String, Object> result = new HashMap<>();
			result.put("success", true);
			result.put("message", "URL analysis saved to history");
			result.put("historyId", history.getId());

			return ResponseEntity.ok(result);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "error", "Failed to save URL analysis: " + e.getMessage()));
		}
	}

	/**
	 * Save email analysis response to history
	 * User clicks "Save to History" button after scanning an email
	 */
	@PostMapping("/save/email")
	public ResponseEntity<?> saveEmailToHistory(@RequestBody Map<String, Object> request, Authentication authentication) {
		try {
			UserCred user = extractUserCredFromAuthentication(authentication);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("success", false, "error", "Could not authenticate user"));
			}

			String email = (String) request.get("email");
			@SuppressWarnings("unchecked")
			Map<String, Object> responseData = (Map<String, Object>) request.get("response");

			if (email == null || email.trim().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(Map.of("success", false, "error", "Email is required"));
			}

			if (responseData == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(Map.of("success", false, "error", "Analysis response is required"));
			}

			// Convert map to EmailAnalysisResponse
			EmailAnalysisResponse response = new com.fasterxml.jackson.databind.ObjectMapper()
					.convertValue(responseData, EmailAnalysisResponse.class);

			// Save to history
			HistoryReport history = historyService.saveEmailAnalysis(email, response, user);

			Map<String, Object> result = new HashMap<>();
			result.put("success", true);
			result.put("message", "Email analysis saved to history");
			result.put("historyId", history.getId());

			return ResponseEntity.ok(result);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "error", "Failed to save email analysis: " + e.getMessage()));
		}
	}

	/**
	 * Save file analysis response to history
	 * Accepts file as multipart/form-data and analysis response as form parameters
	 * 
	 * POST /api/history/save/file
	 * Content-Type: multipart/form-data
	 * 
	 * Body (form-data):
	 *   file: [actual file]
	 *   response: [JSON string of analysis response]
	 *   fileName: [optional - if not provided, uses uploaded file name]
	 *   fileType: [optional - if not provided, uses uploaded file MIME type]
	 */
	@PostMapping("/save/file")
	public ResponseEntity<?> saveFileToHistory(
			@RequestParam(required = false) MultipartFile file,
			@RequestParam(required = false) String response,
			@RequestParam(required = false) String fileName,
			@RequestParam(required = false) String fileType,
			Authentication authentication) {
		try {
			UserCred user = extractUserCredFromAuthentication(authentication);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("success", false, "error", "Could not authenticate user"));
			}

			// Use provided fileName or get from file
			String actualFileName = (fileName != null && !fileName.trim().isEmpty()) ? 
					fileName : 
					(file != null ? file.getOriginalFilename() : null);

			// Use provided fileType or get from file
			String actualFileType = (fileType != null && !fileType.trim().isEmpty()) ? 
					fileType : 
					(file != null ? file.getContentType() : null);

			// Validate fileName
			if (actualFileName == null || actualFileName.trim().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(Map.of("success", false, "error", "File name is required"));
			}

			// Validate response
			if (response == null || response.trim().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(Map.of("success", false, "error", "Analysis response is required"));
			}

			// Parse response JSON string to map
			com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			Map<String, Object> responseData = mapper.readValue(response, Map.class);

			// Convert map to AiAnalysisResponseDto
			AiAnalysisResponseDto analysisResponse = mapper.convertValue(responseData, AiAnalysisResponseDto.class);

			// Get file bytes if file is provided
			byte[] fileBytes = (file != null) ? file.getBytes() : null;

			// Save to history
			HistoryReport history = historyService.saveFileAnalysis(
					actualFileName, 
					actualFileType, 
					fileBytes, 
					analysisResponse, 
					user
			);

			Map<String, Object> result = new HashMap<>();
			result.put("success", true);
			result.put("message", "File analysis saved to history");
			result.put("historyId", history.getId());

			return ResponseEntity.ok(result);
		} catch (com.fasterxml.jackson.core.JsonParseException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("success", false, "error", "Invalid JSON in response parameter: " + e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "error", "Failed to save file analysis: " + e.getMessage()));
		}
	}

	/**
	 * Save description analysis response to history
	 * User clicks "Save to History" button after scanning a description
	 */
	@PostMapping("/save/description")
	public ResponseEntity<?> saveDescriptionToHistory(@RequestBody Map<String, Object> request, Authentication authentication) {
		try {
			UserCred user = extractUserCredFromAuthentication(authentication);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("success", false, "error", "Could not authenticate user"));
			}

			String jobDescription = (String) request.get("jobDescription");
			@SuppressWarnings("unchecked")
			Map<String, Object> responseData = (Map<String, Object>) request.get("response");

			if (jobDescription == null || jobDescription.trim().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(Map.of("success", false, "error", "Job description is required"));
			}

			if (responseData == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(Map.of("success", false, "error", "Analysis response is required"));
			}

			// Convert map to AiAnalysisResponseDto
			AiAnalysisResponseDto response = new com.fasterxml.jackson.databind.ObjectMapper()
					.convertValue(responseData, AiAnalysisResponseDto.class);

			// Save to history
			HistoryReport history = historyService.saveDescriptionAnalysis(jobDescription, response, user);

			Map<String, Object> result = new HashMap<>();
			result.put("success", true);
			result.put("message", "Description analysis saved to history");
			result.put("historyId", history.getId());

			return ResponseEntity.ok(result);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "error", "Failed to save description analysis: " + e.getMessage()));
		}
	}
}