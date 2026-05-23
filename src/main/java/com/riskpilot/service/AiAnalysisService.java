package com.riskpilot.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.riskpilot.model.AiAnalysisRequest;
import com.riskpilot.model.AiAnalysisResponseDto;
import com.riskpilot.model.ReviewAnalysis;

/**
 * Service that integrates with Google Gemini API to analyze job/internship offers
 * for scam indicators and security risks.
 * Also integrates with company review analysis for enhanced decision making.
 */
@Service
public class AiAnalysisService {

	@Value("${gemini.api.key}")
	private String geminiApiKey;

//	@Value("${gemini.model:gemini-1.5-flash}")
//	private String modelName;

	// Google AI Studio Free API Endpoint (no model name in URL)
	private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent";

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final HttpClient httpClient = HttpClient.newHttpClient();

	/**
	 * Sends the extracted text to Gemini API for scam detection analysis.
	 * Returns structured analysis result with risk level, trust score, and indicators.
	 */
	public AiAnalysisResponseDto analyzeWithAi(AiAnalysisRequest aiRequest) {
		return analyzeWithAi(aiRequest, null);
	}

	/**
	 * Enhanced analysis with company review data.
	 * Sends extracted text and review analysis to Gemini API for comprehensive assessment.
	 * 
	 * @param aiRequest The AI analysis request with extracted data
	 * @param reviewAnalysis Optional review analysis data to enhance the decision
	 * @return AiAnalysisResponseDto with comprehensive risk assessment
	 */
	public AiAnalysisResponseDto analyzeWithAi(AiAnalysisRequest aiRequest, ReviewAnalysis reviewAnalysis) {
		try {
			String prompt = buildScamDetectionPrompt(aiRequest, reviewAnalysis);
			String aiResponse = callGeminiApi(prompt);
			AiAnalysisResponseDto result = parseAiResponse(aiResponse);
			
			// Attach review analysis to result
			if (reviewAnalysis != null) {
				result.setReviewAnalysis(reviewAnalysis);
				result.setReviewBasedRiskAdjustment(generateReviewRiskAdjustment(result, reviewAnalysis));
			}
			
			return result;
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Failed to call Gemini API for analysis: " + e.getMessage(), e);
		}
	}

	/**
	 * Builds a detailed prompt for the AI model to detect scam indicators.
	 * Handles both text content and image-based content, with optional review data.
	 */
	private String buildScamDetectionPrompt(AiAnalysisRequest aiRequest, ReviewAnalysis reviewAnalysis) {
		StringBuilder prompt = new StringBuilder();

		prompt.append("Analyze the following job/internship offer text for scam indicators and security risks. ");
		prompt.append("Return ONLY a valid JSON object (no markdown, no code blocks) with the following structure:\n");
		prompt.append("{\n");
		prompt.append("  \"riskLevel\": \"LOW\"|\"MEDIUM\"|\"HIGH\",\n");
		prompt.append("  \"trustScore\": <number between 0-10>,\n");
		prompt.append("  \"email\": \"<extracted email or null>\",\n");
		prompt.append("  \"url\": \"<extracted URL or null>\",\n");
		prompt.append("  \"companyName\": \"<extracted company name from email domain, URL, or description, or null>\",\n");
		prompt.append("  \"emailVerified\": <true if corporate email, false if suspicious, null if not found>,\n");
		prompt.append("  \"companyVerified\": <true if company name is identifiable, false otherwise>,\n");
		prompt.append("  \"companyDetails\": \"<details about the company verification and extracted company name>\",\n");
		prompt.append("  \"suspiciousIndicators\": [<list of detected scam indicators>],\n");
		prompt.append("  \"suggestions\": [<list of recommendations>],\n");
		prompt.append("  \"analysisSummary\": \"<brief summary of the analysis>\"\n");
		prompt.append("}\n\n");

		prompt.append("Look for these scam indicators:\n");
		prompt.append("1. Requests for payment or registration fees before the job starts\n");
		prompt.append("2. Requests for personal documents (Aadhaar, PAN, bank details, SSN, passport)\n");
		prompt.append("3. Shortened or suspicious URLs (bit.ly, tinyurl, IP-based URLs)\n");
		prompt.append("4. Urgency tactics (\"within 24 hours\", \"limited seats\", \"hurry\")\n");
		prompt.append("5. Fake or impersonated company names\n");
		prompt.append("6. Non-corporate email domains (gmail.com, yahoo.com, hotmail.com)\n");
		prompt.append("7. Disposable/temporary email services (tempmail, guerrillamail)\n");
		prompt.append("8. Promises of high salary with minimal work\n");
		prompt.append("9. \"Work from home\" guarantees\n");
		prompt.append("10. Requests to contact via WhatsApp, Telegram, or other messaging apps\n");
		prompt.append("11. Suspicious payment methods (UPI, Google Pay, PayTM)\n");
		prompt.append("12. No verifiable company information\n\n");

		prompt.append("IMPORTANT: Return ONLY valid JSON, no additional text or markdown formatting.\n");
		prompt.append("If you cannot provide valid JSON, still return JSON structure with generic messages.\n\n");

		prompt.append("=== CONTENT TO ANALYZE ===\n");
		
		// Check if content is image data
		String extractedText = aiRequest.getExtractedText();
		if (extractedText != null && extractedText.startsWith("IMAGE_BASE64:")) {
			// For image content, add the image marker and instructions
			prompt.append("The following image contains the job/internship offer. Please analyze it carefully.\n");
			prompt.append("Read the text from the image and look for scam indicators.\n\n");
			prompt.append(extractedText).append("\n\n");
		} else {
			// For text content
			prompt.append(extractedText).append("\n\n");
		}

		if (aiRequest.getEmail() != null && !aiRequest.getEmail().isEmpty()) {
			prompt.append("Extracted Email: ").append(aiRequest.getEmail()).append("\n");
		}
		if (aiRequest.getCompanyName() != null && !aiRequest.getCompanyName().isEmpty()) {
			prompt.append("Extracted Company: ").append(aiRequest.getCompanyName()).append("\n");
		}
		if (aiRequest.getUrl() != null && !aiRequest.getUrl().isEmpty()) {
			prompt.append("Extracted URL: ").append(aiRequest.getUrl()).append("\n");
		}
		
		// NEW: Add review analysis context
		if (reviewAnalysis != null && reviewAnalysis.hasReviews()) {
			prompt.append("\n=== COMPANY REPUTATION DATA (from online reviews) ===\n");
			prompt.append("Company: ").append(reviewAnalysis.getCompanyName()).append("\n");
			prompt.append("Average Rating: ").append(reviewAnalysis.getAverageRating()).append("/5.0\n");
			prompt.append("Total Reviews: ").append(reviewAnalysis.getTotalReviews()).append("\n");
			prompt.append("Positive: ").append(reviewAnalysis.getPositiveCount()).append(" | ");
			prompt.append("Negative: ").append(reviewAnalysis.getNegativeCount()).append(" | ");
			prompt.append("Neutral: ").append(reviewAnalysis.getNeutralCount()).append("\n");
			prompt.append("Overall Sentiment: ").append(reviewAnalysis.getOverallSentiment()).append("\n");
			prompt.append("Status: ").append(reviewAnalysis.getReviewHealthStatus()).append("\n");
			prompt.append("Summary: ").append(reviewAnalysis.getSummaryAnalysis()).append("\n\n");
			
			// Add key reviews
			if (reviewAnalysis.getTopReviews() != null && !reviewAnalysis.getTopReviews().isEmpty()) {
				prompt.append("Sample Reviews:\n");
				reviewAnalysis.getTopReviews().stream().limit(3).forEach(review -> {
					prompt.append("- [").append(review.getRating()).append("/5] ")
						.append(review.getSentiment()).append(": ")
						.append(review.getReviewText().substring(0, Math.min(100, review.getReviewText().length())))
						.append("...\n");
				});
			}
			
			prompt.append("\nCONTEXT: Use this company reputation data as additional validation. ");
			prompt.append("If the company has good reviews, it may be more trustworthy. ");
			prompt.append("If the company has poor reviews or many complaints about scams, it is more suspicious.\n\n");
		}

		return prompt.toString();
	}
	
	/**
	 * Generates a text explaining how reviews affected the risk assessment.
	 */
	private String generateReviewRiskAdjustment(AiAnalysisResponseDto aiResponse, ReviewAnalysis reviewAnalysis) {
		StringBuilder adjustment = new StringBuilder();
		
		if (reviewAnalysis.getTotalReviews() == 0) {
			adjustment.append("⚠️ No online reviews found for this company.");
		} else {
			adjustment.append("📊 Review Analysis: ");
			
			if (reviewAnalysis.getAverageRating() >= 4.0) {
				adjustment.append("Company has good reputation (").append(reviewAnalysis.getAverageRating()).append("/5). ");
				adjustment.append("This slightly increases trust, but does not guarantee legitimacy of this specific offer.");
			} else if (reviewAnalysis.getAverageRating() >= 3.0) {
				adjustment.append("Company has mixed reputation (").append(reviewAnalysis.getAverageRating()).append("/5). ");
				adjustment.append("Some concerns have been reported by users.");
			} else if (reviewAnalysis.getTotalReviews() > 0) {
				adjustment.append("⚠️ Company has poor reputation (").append(reviewAnalysis.getAverageRating()).append("/5). ");
				adjustment.append("Multiple negative reviews detected. Exercise extra caution.");
			}
			
			if (reviewAnalysis.getNegativeCount() > (reviewAnalysis.getTotalReviews() * 0.4)) {
				adjustment.append("\n🚨 ALERT: ").append(reviewAnalysis.getNegativePercentage()).append("% of reviews are negative.");
			}
		}
		
		return adjustment.toString();
	}

	// ... rest of the methods remain the same ...

	/**
	 * Calls the Google Gemini API with the prompt and returns the response text.
	 * Supports both text and image analysis using Gemini Vision API.
	 * Implements exponential backoff retry logic for handling temporary API failures.
	 */
	private String callGeminiApi(String prompt) throws IOException, InterruptedException {
		final int MAX_RETRIES = 3;
		final long INITIAL_DELAY_MS = 1000; // 1 second
		long delayMs = INITIAL_DELAY_MS;
		
		for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
			try {
				return callGeminiApiInternal(prompt);
			} catch (RuntimeException e) {
				// Check if it's a 503 (Service Unavailable) error
				if (e.getMessage() != null && e.getMessage().contains("\"code\": 503")) {
					if (attempt < MAX_RETRIES) {
						System.out.println("Gemini API temporarily unavailable (attempt " + attempt + "/" + MAX_RETRIES + "). Retrying in " + delayMs + "ms...");
						Thread.sleep(delayMs);
						delayMs *= 2; // Exponential backoff: double the delay each time
						continue;
					} else {
						System.out.println("Gemini API unavailable after " + MAX_RETRIES + " attempts. Please try again later.");
					}
				}
				// For non-503 errors or final attempt, throw the exception
				throw e;
			}
		}
		
		// Should not reach here, but just in case
		throw new RuntimeException("Failed to call Gemini API after " + MAX_RETRIES + " attempts");
	}

	/**
	 * Internal method that performs the actual Gemini API call.
	 * Called by callGeminiApi with retry logic wrapping.
	 */
	private String callGeminiApiInternal(String prompt) throws IOException, InterruptedException {
		String apiUrl = GEMINI_API_URL + "?key=" + geminiApiKey;

		// Build the request body for Gemini API
		ObjectNode contentNode = objectMapper.createObjectNode();
		ArrayNode partsArray = objectMapper.createArrayNode();
		
		// Check if the prompt contains image data in IMAGE_BASE64 format
		if (prompt.contains("IMAGE_BASE64:")) {
			// Extract image data and build request with image
			int imageMarkerIndex = prompt.indexOf("IMAGE_BASE64:");
			String textPart = prompt.substring(0, imageMarkerIndex).trim();
			
			// Everything after "IMAGE_BASE64:" should be parsed as: mediaType:base64String:restOfPrompt
			String afterMarker = prompt.substring(imageMarkerIndex + "IMAGE_BASE64:".length());
			
			// Split on first two colons only to separate mediaType and base64 image
			String[] firstSplit = afterMarker.split(":", 2);
			if (firstSplit.length < 2) {
				throw new IllegalArgumentException("Invalid image data format. Expected IMAGE_BASE64:mediaType:base64String");
			}
			
			String mediaType = firstSplit[0].trim();
			String remainingContent = firstSplit[1];
			
			// The remaining content starts with the base64 string followed by the rest of the prompt
			// Find where the base64 string ends (it should only contain alphanumeric chars, +, /, and =)
			// But since we need to separate base64 from text, find the last continuous base64 sequence
			String base64Image = extractBase64ImageData(remainingContent);
			
			// Reconstruct the analysis prompt without the image data
			String analysisInstructions = textPart + "\n\n" + extractPostImagePrompt(remainingContent, base64Image);
			
			// Add text part with analysis instructions
			ObjectNode textNode = objectMapper.createObjectNode();
			textNode.put("text", analysisInstructions);
			partsArray.add(textNode);
			
			// Add image part
			ObjectNode imageNode = objectMapper.createObjectNode();
			ObjectNode inlineData = objectMapper.createObjectNode();
			inlineData.put("mime_type", mediaType);
			inlineData.put("data", base64Image);
			imageNode.set("inline_data", inlineData);
			partsArray.add(imageNode);
		} else {
			// Text-only analysis
			ObjectNode partNode = objectMapper.createObjectNode();
			partNode.put("text", prompt);
			partsArray.add(partNode);
		}
		
		contentNode.set("parts", partsArray);

		ArrayNode contentsArray = objectMapper.createArrayNode();
		contentsArray.add(contentNode);

		ObjectNode requestBody = objectMapper.createObjectNode();
		requestBody.set("contents", contentsArray);

		// Optional: Add safety settings to minimize filtering
		ObjectNode safetySettings = objectMapper.createObjectNode();
		ArrayNode safetyArray = objectMapper.createArrayNode();
		requestBody.set("safety_settings", safetyArray);

		String jsonBody = objectMapper.writeValueAsString(requestBody);

		// Make HTTP request
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(apiUrl))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			throw new RuntimeException("Gemini API error: " + response.body());
		}

		// Parse and extract the text content from Gemini's response
		JsonNode responseJson = objectMapper.readTree(response.body());
		String extractedText = responseJson
				.path("candidates")
				.get(0)
				.path("content")
				.path("parts")
				.get(0)
				.path("text")
				.asText();

		return extractedText;
	}

	/**
	 * Parses the AI response JSON and converts it to AiAnalysisResponseDto.
	 * Handles JSON extraction and error cases gracefully.
	 */
	private AiAnalysisResponseDto parseAiResponse(String aiResponse) {
		try {
			// Try to find JSON in the response (it might be wrapped in markdown or text)
			String jsonString = extractJsonFromResponse(aiResponse);

			JsonNode jsonNode = objectMapper.readTree(jsonString);

			AiAnalysisResponseDto result = new AiAnalysisResponseDto();

		// Parse risk level
			String riskLevel = jsonNode.path("riskLevel").asText("MEDIUM");
			result.setRiskLevel(riskLevel);

			// Parse trust score (convert from 0-10 scale to ensure it's in the right range)
			double trustScore = jsonNode.path("trustScore").asDouble(5.0);
			result.setTrustScore(Math.max(0, Math.min(10, trustScore))); // Clamp between 0-10

			// Parse email
			String email = jsonNode.path("email").asText(null);
			result.setEmail(email);

			// Parse URL
			String url = jsonNode.path("url").asText(null);
			result.setUrl(url);

			// Parse company name (and remove quotes if present)
			String companyName = jsonNode.path("companyName").asText(null);
			result.setCompanyName(removeQuotes(companyName));

			// Parse email verified flag
			Boolean emailVerified = jsonNode.path("emailVerified").isNull() ? null : jsonNode.path("emailVerified").asBoolean();
			result.setEmailVerified(emailVerified);

			// Parse company verified flag
			Boolean companyVerified = jsonNode.path("companyVerified").isNull() ? null : jsonNode.path("companyVerified").asBoolean();
			result.setCompanyVerified(companyVerified);

			// Parse company details (and remove quotes if present)
			String companyDetails = jsonNode.path("companyDetails").asText(null);
			result.setCompanyDetails(removeQuotes(companyDetails));

			// Parse suspicious indicators
			List<String> indicators = new ArrayList<>();
			JsonNode indicatorsNode = jsonNode.path("suspiciousIndicators");
			if (indicatorsNode.isArray()) {
				indicatorsNode.forEach(node -> indicators.add(node.asText()));
			}
			result.setSuspiciousIndicators(indicators);

			// Parse suggestions
			List<String> suggestions = new ArrayList<>();
			JsonNode suggestionsNode = jsonNode.path("suggestions");
			if (suggestionsNode.isArray()) {
				suggestionsNode.forEach(node -> suggestions.add(node.asText()));
			}
			result.setSuggestions(suggestions);

			// Parse analysis summary
			String summary = jsonNode.path("analysisSummary").asText("Analysis completed by AI model.");
			result.setAnalysisSummary(summary);

			return result;
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
		}
	}

	/**
	 * Extracts JSON from the response. Handles cases where JSON might be wrapped
	 * in markdown code blocks or surrounded by text.
	 */
	private String extractJsonFromResponse(String response) {
		// Remove markdown code blocks if present
		String cleaned = response.replaceAll("```json\\s*", "")
				.replaceAll("```\\s*", "");

		// Try to find JSON object
		int startIndex = cleaned.indexOf('{');
		int endIndex = cleaned.lastIndexOf('}');

		if (startIndex >= 0 && endIndex > startIndex) {
			return cleaned.substring(startIndex, endIndex + 1);
		}

		// If no JSON found, return as-is (will likely fail parsing and throw exception)
		return cleaned;
	}

	/**
	 * Extracts the Base64 image data from the content string.
	 * Base64 strings consist of alphanumeric characters, +, /, and = padding.
	 * Stops when a non-Base64 character is encountered.
	 */
	private String extractBase64ImageData(String content) {
		// Base64 valid characters: A-Z, a-z, 0-9, +, /, and = (for padding)
		StringBuilder base64 = new StringBuilder();
		
		for (int i = 0; i < content.length(); i++) {
			char c = content.charAt(i);
			// Valid Base64 characters
			if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') 
					|| c == '+' || c == '/' || c == '=') {
				base64.append(c);
			} else {
				// Stop at first non-Base64 character (usually newline or space)
				break;
			}
		}
		
		return base64.toString();
	}

	/**
	 * Extracts the remaining prompt text that comes after the Base64 image data.
	 * This includes the analysis instructions that should be sent with the image.
	 */
	private String extractPostImagePrompt(String content, String base64Image) {
		// Find where the base64 image data ends in the content
		int base64EndIndex = content.indexOf(base64Image) + base64Image.length();
		
		if (base64EndIndex < content.length()) {
			String remaining = content.substring(base64EndIndex).trim();
			// Return the remaining content (analysis instructions)
			return remaining.isEmpty() ? "" : remaining;
		}
		
		return "";
	}

	/**
	 * Removes surrounding single or double quotes from a string.
	 * Handles cases where AI returns values wrapped in quotes.
	 * 
	 * Examples:
	 * - "'Amazon'" → "Amazon"
	 * - '"Microsoft"' → "Microsoft"
	 * - 'TechCorp Inc' → TechCorp Inc
	 * - null → null
	 */
	private String removeQuotes(String value) {
		if (value == null || value.isBlank()) {
			return value;
		}
		
		String trimmed = value.trim();
		
		// Remove double quotes
		if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
			trimmed = trimmed.substring(1, trimmed.length() - 1);
		}
		
		// Remove single quotes
		if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2) {
			trimmed = trimmed.substring(1, trimmed.length() - 1);
		}
		
		return trimmed.isBlank() ? null : trimmed;
	}
}