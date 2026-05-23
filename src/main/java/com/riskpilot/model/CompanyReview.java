package com.riskpilot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single online review for a company.
 * Contains review text, rating, and metadata.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyReview {
	
	private String reviewerName;      // Name of the reviewer
	private String reviewText;        // Full review text
	private double rating;            // Rating (1-5 stars)
	private String source;            // Source: "Google", "Yelp", "Trustpilot", etc.
	private String sentiment;         // "POSITIVE", "NEGATIVE", "NEUTRAL"
	private long reviewDate;          // Timestamp of review
	private String reviewUrl;         // Link to the review
	
	// Quick helper method to get sentiment emoji
	public String getSentimentEmoji() {
		if ("POSITIVE".equals(sentiment)) return "👍 ";
		if ("NEGATIVE".equals(sentiment)) return "⚠️ ";
		return "➖ ";
	}
}
