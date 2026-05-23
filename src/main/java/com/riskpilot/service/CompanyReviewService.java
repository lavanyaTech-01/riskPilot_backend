package com.riskpilot.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.riskpilot.model.CompanyReview;
import com.riskpilot.model.ReviewAnalysis;

/**
 * Service for fetching and analyzing company reviews from online sources.
 * 
 * Currently implements:
 * - Mock data for demonstration
 * - Sentiment analysis based on review keywords
 * - Review aggregation and scoring
 * 
 * Future integrations:
 * - Google Places API
 * - Yelp API
 * - Trustpilot API
 * - LinkedIn Company Reviews
 */
@Service
public class CompanyReviewService {
	
	private static final int FETCH_LIMIT = 10; // Limit reviews to top 10
	
	/**
	 * Fetches company reviews from online sources and performs sentiment analysis.
	 * 
	 * @param companyName The company name to search for
	 * @return ReviewAnalysis with aggregated ratings and reviews
	 */
	public ReviewAnalysis fetchAndAnalyzeCompanyReviews(String companyName) {
		if (companyName == null || companyName.trim().isEmpty()) {
			System.out.println("⚠️ Company name is null or empty");
			return createEmptyReviewAnalysis("Unknown Company");
		}
		
		System.out.println("🔍 Searching for reviews for: " + companyName);
		
		try {
			// Step 1: Try to fetch reviews (currently using mock data)
			List<CompanyReview> reviews = fetchReviewsFromSources(companyName);
			
			System.out.println("📊 Found " + reviews.size() + " reviews for: " + companyName);
			
			// Step 2: If no reviews found, return empty analysis
			if (reviews == null || reviews.isEmpty()) {
				System.out.println("⚠️ No reviews found for: " + companyName);
				return createEmptyReviewAnalysis(companyName);
			}
			
			// Step 3: Analyze and aggregate reviews
			return analyzeReviews(companyName, reviews);
			
		} catch (Exception e) {
			System.err.println("❌ Error fetching company reviews for " + companyName + ": " + e.getMessage());
			e.printStackTrace();
			return createEmptyReviewAnalysis(companyName);
		}
	}
	
	/**
	 * Fetches reviews from multiple sources.
	 * Currently returns mock/demo data. Can be extended with real API calls.
	 */
	private List<CompanyReview> fetchReviewsFromSources(String companyName) {
		List<CompanyReview> allReviews = new ArrayList<>();
		
		// Try multiple sources (extensible for real APIs)
		allReviews.addAll(fetchFromGooglePlaces(companyName));
		allReviews.addAll(fetchFromJustDial(companyName));
		allReviews.addAll(fetchFromGlassdoor(companyName));
		
		// Deduplicate and limit to top results
		return allReviews.stream()
				.limit(FETCH_LIMIT)
				.collect(Collectors.toList());
	}
	
	/**
	 * Fetch from Google Places API (placeholder for actual implementation)
	 */
	private List<CompanyReview> fetchFromGooglePlaces(String companyName) {
		// TODO: Implement actual Google Places API integration
		// String apiKey = System.getenv("GOOGLE_PLACES_API_KEY");
		// Make HTTP call to Google Places API
		// Parse and return reviews
		
		// For now, return mock data
		return getMockReviews(companyName, "Google");
	}
	
	/**
	 * Fetch from JustDial (placeholder for actual implementation)
	 */
	private List<CompanyReview> fetchFromJustDial(String companyName) {
		// TODO: Implement actual JustDial API integration
		// Make HTTP call to JustDial API
		// Parse and return reviews
		
		return new ArrayList<>();
	}
	
	/**
	 * Fetch from Glassdoor (placeholder for actual implementation)
	 */
	private List<CompanyReview> fetchFromGlassdoor(String companyName) {
		// TODO: Implement actual Glassdoor API integration
		// Make HTTP call to Glassdoor API
		// Parse and return reviews
		
		return new ArrayList<>();
	}
	
	/**
	 * Analyzes and aggregates reviews to produce ReviewAnalysis object.
	 */
	private ReviewAnalysis analyzeReviews(String companyName, List<CompanyReview> reviews) {
		if (reviews.isEmpty()) {
			return createEmptyReviewAnalysis(companyName);
		}
		
		// Calculate statistics
		double averageRating = reviews.stream()
				.mapToDouble(CompanyReview::getRating)
				.average()
				.orElse(0);
		
		int positiveCount = (int) reviews.stream()
				.filter(r -> "POSITIVE".equals(r.getSentiment()))
				.count();
		
		int negativeCount = (int) reviews.stream()
				.filter(r -> "NEGATIVE".equals(r.getSentiment()))
				.count();
		
		int neutralCount = (int) reviews.stream()
				.filter(r -> "NEUTRAL".equals(r.getSentiment()))
				.count();
		
		// Determine overall sentiment
		String overallSentiment = determineOverallSentiment(positiveCount, negativeCount, neutralCount);
		
		// Determine risk indicator based on reviews
		String riskIndicator = determineRiskIndicator(averageRating, negativeCount, reviews.size());
		
		// Determine source(s)
		String source = determineSources(reviews);
		
		// Build review analysis
		ReviewAnalysis analysis = new ReviewAnalysis();
		analysis.setCompanyName(companyName);
		analysis.setAverageRating(Math.round(averageRating * 10.0) / 10.0); // Round to 1 decimal
		analysis.setTotalReviews(reviews.size());
		analysis.setPositiveCount(positiveCount);
		analysis.setNegativeCount(negativeCount);
		analysis.setNeutralCount(neutralCount);
		analysis.setOverallSentiment(overallSentiment);
		analysis.setTopReviews(new ArrayList<>()); // Empty - don't show individual reviews
		analysis.setSource(source);
		analysis.setRiskIndicator(riskIndicator);
		analysis.setSummaryAnalysis(generateReviewSummary(reviews, averageRating, overallSentiment));
		
		return analysis;
	}
	
	/**
	 * Determines the source(s) of reviews.
	 */
	private String determineSources(List<CompanyReview> reviews) {
		java.util.Set<String> sources = new java.util.HashSet<>();
		reviews.forEach(r -> sources.add(r.getSource()));
		
		if (sources.isEmpty()) {
			return "None";
		} else if (sources.size() == 1) {
			return sources.iterator().next();
		} else {
			return String.join(", ", sources);
		}
	}
	
	/**
	 * Determines overall sentiment based on review distribution.
	 */
	private String determineOverallSentiment(int positive, int negative, int neutral) {
		int total = positive + negative + neutral;
		if (total == 0) return "UNKNOWN";
		
		if (positive > (total * 0.6)) return "POSITIVE";
		if (negative > (total * 0.4)) return "NEGATIVE";
		return "MIXED";
	}
	
	/**
	 * Determines risk indicator based on review metrics.
	 */
	private String determineRiskIndicator(double averageRating, int negativeCount, int totalReviews) {
		if (totalReviews == 0) return "UNKNOWN";
		
		// High negative percentage is a red flag
		if (negativeCount > (totalReviews * 0.5)) return "FLAGGED";
		
		// Low average rating is suspicious
		if (averageRating < 2.0) return "SUSPICIOUS";
		
		// Good rating = legitimate
		if (averageRating >= 3.5) return "LEGITIMATE";
		
		return "SUSPICIOUS";
	}
	
	/**
	 * Generates a summary analysis of the reviews.
	 */
	private String generateReviewSummary(List<CompanyReview> reviews, double avgRating, String sentiment) {
		StringBuilder summary = new StringBuilder();
		
		summary.append("Company has ").append(reviews.size()).append(" reviews with average rating of ").append(avgRating).append("/5. ");
		summary.append("Overall sentiment: ").append(sentiment).append(". ");
		
		if (reviews.isEmpty()) {
			summary.append("No detailed review information available.");
		} else {
			// Add key themes from reviews
			long positiveCount = reviews.stream().filter(r -> "POSITIVE".equals(r.getSentiment())).count();
			long negativeCount = reviews.stream().filter(r -> "NEGATIVE".equals(r.getSentiment())).count();
			
			if (positiveCount > negativeCount) {
				summary.append("Most users report positive experiences.");
			} else if (negativeCount > positiveCount) {
				summary.append("Multiple users report negative experiences or concerns.");
			} else {
				summary.append("Reviews are mixed with both positive and negative feedback.");
			}
		}
		
		return summary.toString();
	}
	
	/**
	 * Creates an empty review analysis when no reviews are found.
	 */
	private ReviewAnalysis createEmptyReviewAnalysis(String companyName) {
		ReviewAnalysis analysis = new ReviewAnalysis();
		analysis.setCompanyName(companyName);
		analysis.setAverageRating(0);
		analysis.setTotalReviews(0);
		analysis.setPositiveCount(0);
		analysis.setNegativeCount(0);
		analysis.setNeutralCount(0);
		analysis.setOverallSentiment("UNKNOWN");
		analysis.setTopReviews(new ArrayList<>());
		analysis.setSource("None");
		analysis.setRiskIndicator("UNKNOWN");
		analysis.setSummaryAnalysis("No company reviews found online.");
		return analysis;
	}
	
	/**
	 * Performs sentiment analysis on review text.
	 * Uses keyword matching for now, can be enhanced with ML models.
	 */
	private String analyzeSentiment(String reviewText, double rating) {
		if (rating >= 4.0) return "POSITIVE";
		if (rating <= 2.0) return "NEGATIVE";
		
		// Keyword-based sentiment for edge cases
		String text = reviewText.toLowerCase();
		
		long positiveKeywords = java.util.stream.Stream.of(
				"excellent", "great", "amazing", "wonderful", "good", "best", "love",
				"satisfied", "professional", "efficient", "reliable", "trustworthy"
		).filter(text::contains).count();
		
		long negativeKeywords = java.util.stream.Stream.of(
				"terrible", "awful", "horrible", "worst", "bad", "scam", "fraud",
				"unsatisfied", "waste", "poor", "unreliable", "fake", "fake"
		).filter(text::contains).count();
		
		if (positiveKeywords > negativeKeywords) return "POSITIVE";
		if (negativeKeywords > positiveKeywords) return "NEGATIVE";
		
		return "NEUTRAL";
	}
	
	/**
	 * Mock data for demonstration purposes.
	 * This simulates API responses and will be replaced with actual API calls.
	 */
	private List<CompanyReview> getMockReviews(String companyName, String source) {
		List<CompanyReview> mockReviews = new ArrayList<>();
		
		// Return mock data for ANY company (simulating real API)
		// Generate varied reviews based on company name hash
		int hashCode = companyName.toLowerCase().hashCode();
		int reviewCount = 2 + Math.abs(hashCode % 3);
		
		String[] reviewerNames = {"John Doe", "Jane Smith", "Mike Johnson", "Sarah Williams", "Ahmed Khan"};
		String[] positiveReviews = {
			"Great experience working with this company. Very professional and trustworthy.",
			"Good workplace with excellent benefits and career growth opportunities.",
			"Excellent company culture and supportive management team.",
			"Outstanding pay and benefits. Highly recommend!",
			"Best company I've worked for. Great work-life balance."
		};
		String[] negativeReviews = {
			"Poor management and unfair treatment of employees.",
			"Salary doesn't match the workload. Very demanding.",
			"Lack of career growth opportunities. Stagnant position.",
			"Difficult management, high stress environment.",
			"False promises during hiring. Not worth it."
		};
		
		for (int i = 0; i < reviewCount; i++) {
			double rating = 3.5 + (Math.abs(hashCode * (i + 1)) % 20) / 10.0;
			String sentiment = rating >= 4.0 ? "POSITIVE" : rating <= 2.5 ? "NEGATIVE" : "NEUTRAL";
			String reviewText = rating >= 4.0 ? 
				positiveReviews[Math.abs(hashCode * i) % positiveReviews.length] :
				negativeReviews[Math.abs(hashCode * i) % negativeReviews.length];
			
			mockReviews.add(new CompanyReview(
				reviewerNames[Math.abs(hashCode * i) % reviewerNames.length],
				reviewText,
				rating,
				source,
				sentiment,
				Instant.now().getEpochSecond() - (86400 * i),
				"https://example.com/review/" + i
			));
		}
		
		return mockReviews;
	}
}
