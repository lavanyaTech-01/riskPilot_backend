package com.riskpilot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for analyzing job/internship descriptions.
 * Accepts raw description text and performs comprehensive analysis
 * including email extraction, URL verification, and AI-powered scam detection.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DescriptionAnalysisRequest {
	
	private String jobDescription;  // Full job/internship description text
}
