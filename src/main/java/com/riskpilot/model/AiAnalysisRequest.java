package com.riskpilot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending text to the AI model for scam detection analysis.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiAnalysisRequest {

	private String extractedText;    // Raw text extracted from the PDF file
	
	private String email;             // Email extracted from the content
	
	private String companyName;       // Company name extracted from the content
	
	private String url;               // URL extracted from the content
}
