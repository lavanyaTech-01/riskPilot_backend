package com.riskpilot.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated review analysis for a company.
 * Contains overall ratings, sentiment distribution, and individual reviews.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewAnalysis {
	
	private String companyName;           // Company name
	private double averageRating;         // Average star rating (0-5)
	private int totalReviews;             // Total number of reviews found
	private int positiveCount;            // Number of positive reviews
	private int negativeCount;            // Number of negative reviews
	private int neutralCount;             // Number of neutral reviews
	private String overallSentiment;      // "POSITIVE", "NEGATIVE", "MIXED", "UNKNOWN"
	private List<CompanyReview> topReviews;  // Top 5-10 reviews
	private String source;                // Primary source: "Google", "Yelp", "Trustpilot", "Multiple"
	private String riskIndicator;         // "LEGITIMATE", "SUSPICIOUS", "FLAGGED", "UNKNOWN"
	private String summaryAnalysis;       // AI-generated summary of reviews
	
	// Helper methods
	public boolean hasReviews() {
		return totalReviews > 0;
	}
	
	public double getPositivePercentage() {
		if (totalReviews == 0) return 0;
		return (positiveCount * 100.0) / totalReviews;
	}
	
	public double getNegativePercentage() {
		if (totalReviews == 0) return 0;
		return (negativeCount * 100.0) / totalReviews;
	}
	
	public String getReviewHealthStatus() {
		if (totalReviews == 0) return "❓ No reviews found";
		if (averageRating >= 4.0) return "✅ Good reputation";
		if (averageRating >= 3.0) return "⚠️ Mixed reputation";
		return "🚨 Poor reputation";
	}
}
