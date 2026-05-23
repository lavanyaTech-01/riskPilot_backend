package com.riskpilot.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO class that maps the structured JSON response from Google Gemini API.
 * The AI model returns analysis results in this format, including review analysis.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiAnalysisResponseDto {

	private String riskLevel;        // "LOW", "MEDIUM", or "HIGH"
	
	private double trustScore;       // 0-10 scale
	
	private String email;            // Extracted email from content
	
	private String url;              // Extracted URL from content (if provided)
	
	private String companyName;      // Extracted company name from email domain, URL, or description
	
	private Boolean emailVerified;   // Whether email is from a verified corporate domain
	
	private Boolean companyVerified; // Whether company name was identified
	
	private String companyDetails;   // Details about the company
	
	private List<String> suspiciousIndicators;  // List of detected scam indicators
	
	private List<String> suggestions;           // List of recommendations for the user
	
	private String analysisSummary;  // Summary of the analysis
	
	// NEW: Review Analysis Section
	private ReviewAnalysis reviewAnalysis;  // Online reviews and reputation analysis
	
	private String reviewBasedRiskAdjustment;  // How reviews affected the risk assessment
}