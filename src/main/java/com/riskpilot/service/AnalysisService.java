package com.riskpilot.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.riskpilot.model.AiAnalysisRequest;
import com.riskpilot.model.AiAnalysisResponseDto;
import com.riskpilot.model.ReviewAnalysis;
import com.riskpilot.model.UrlAnalysisRequest;
import com.riskpilot.model.UrlAnalysisResponse;

@Service
public class AnalysisService {

    @Autowired
    private FileExtractionService fileExtractionService;

    @Autowired
    private ContentParserService contentParserService;

    @Autowired
    private AiAnalysisService aiAnalysisService;

    @Autowired
    private UrlAnalysisService urlAnalysisService;
    
    @Autowired
    private CompanyReviewService companyReviewService;

    /**
     * Main method that orchestrates the full AI-powered analysis pipeline:
     * 1. Extract text from the uploaded file (PDF, image, or text)
     * 2. Parse extracted text into structured fields (email, URL, company name)
     * 3. Fetch online reviews for the detected company
     * 4. Perform URL verification if a URL is found
     * 5. Send to AI for scam detection analysis (with review context)
     * 6. Return combined analysis response
     */
    public AiAnalysisResponseDto scanFile(MultipartFile file) {
        // Step 1: Extract text from the uploaded file (PDF, image, or text)
        String extractedText = fileExtractionService.extractText(file);

        if (extractedText == null || extractedText.isBlank()) {
            throw new RuntimeException("Could not extract any text from the uploaded file.");
        }

        // Step 2: Parse content to identify structured fields
        String email = contentParserService.extractAllEmails(extractedText)
            .stream()
            .findFirst()
            .orElse(null);
        
        String url = contentParserService.extractAllUrls(extractedText)
            .stream()
            .findFirst()
            .orElse(null);
        
        System.out.println("✓ Extracted - Email: " + email + ", URL: " + url);
        
        // Step 2.5: Initialize company name (will be set later with better priority)
        String companyName = null;

        // Step 3: NEW - Fetch company reviews if company name is identified
        ReviewAnalysis reviewAnalysis = null;
        if (companyName != null && !companyName.isBlank()) {
            try {
                System.out.println("🔍 Fetching reviews for company: " + companyName);
                reviewAnalysis = companyReviewService.fetchAndAnalyzeCompanyReviews(companyName);
                
                if (reviewAnalysis != null && reviewAnalysis.hasReviews()) {
                    System.out.println("✅ Found " + reviewAnalysis.getTotalReviews() + " reviews for " + companyName);
                } else {
                    System.out.println("⚠️ No reviews found for " + companyName);
                }
            } catch (Exception e) {
                // Log error but continue with analysis
                System.err.println("⚠️ Failed to fetch company reviews: " + e.getMessage());
            }
        } else {
            System.out.println("⚠️ No company name detected - skipping review fetch");
        }

        // Step 4: If URL is found, perform URL verification
        UrlAnalysisResponse urlAnalysisResponse = null;
        if (url != null && !url.isBlank()) {
            try {
                UrlAnalysisRequest urlRequest = new UrlAnalysisRequest();
                urlRequest.setUrl(url);
                urlRequest.setCompanyName(companyName);
                urlRequest.setSenderEmail(email);
                
                urlAnalysisResponse = urlAnalysisService.analyzeUrl(urlRequest);
                
                // If URL has high risk, it may influence overall analysis
                // This can be logged or used for additional decision making
            } catch (Exception e) {
                // Log URL analysis error but continue with AI analysis
                System.err.println("URL analysis failed for " + url + ": " + e.getMessage());
            }
        }

        // Step 5: Build AI request with extracted data
        AiAnalysisRequest aiRequest = new AiAnalysisRequest();
        aiRequest.setExtractedText(extractedText);
        aiRequest.setEmail(email);
        aiRequest.setCompanyName(companyName);
        aiRequest.setUrl(url);

        // Step 6: Perform AI-based scam detection analysis WITH review context
        AiAnalysisResponseDto aiResponse = aiAnalysisService.analyzeWithAi(aiRequest, reviewAnalysis);

        // Step 6.5: Extract company name with new priority order:
        // PRIORITY 1: Email domain (most reliable - verified contact)
        // PRIORITY 2: URL domain (verified website)
        // PRIORITY 3: AI response companyDetails (may have quotes/formatting)
        // PRIORITY 4: Content parsing (last resort)
        if (companyName == null || companyName.isBlank()) {
            
            // PRIORITY 1: Extract from email domain (HIGHEST)
            if (email != null && !email.isBlank()) {
                String emailCompanyName = extractCompanyFromEmail(email);
                if (emailCompanyName != null && !emailCompanyName.isBlank()) {
                    System.out.println("✅ PRIORITY 1 - Company detected from email domain: " + emailCompanyName);
                    companyName = emailCompanyName;
                }
            }
            
            // PRIORITY 2: Extract from URL domain (if email didn't find it)
            if ((companyName == null || companyName.isBlank()) && url != null && !url.isBlank()) {
                String urlCompanyName = extractCompanyFromUrl(url);
                if (urlCompanyName != null && !urlCompanyName.isBlank()) {
                    System.out.println("✅ PRIORITY 2 - Company detected from URL domain: " + urlCompanyName);
                    companyName = urlCompanyName;
                }
            }
            
            // PRIORITY 3: Extract from AI response companyDetails (if email and URL didn't find it)
            if ((companyName == null || companyName.isBlank()) && aiResponse != null && aiResponse.getCompanyDetails() != null && !aiResponse.getCompanyDetails().isBlank()) {
                String aiCompanyName = extractCompanyNameFromDetails(aiResponse.getCompanyDetails());
                if (aiCompanyName != null && !aiCompanyName.isBlank()) {
                    System.out.println("✅ PRIORITY 3 - Company detected from AI response: " + aiCompanyName);
                    companyName = aiCompanyName;
                }
            }
            
            // PRIORITY 4: Content parsing (last resort)
            if (companyName == null || companyName.isBlank()) {
                String companyFromContent = contentParserService.extractCompanyName(extractedText);
                if (companyFromContent != null && !companyFromContent.isBlank()) {
                    System.out.println("✅ PRIORITY 4 - Company detected from content parsing: " + companyFromContent);
                    companyName = companyFromContent;
                }
            }
            
            // If we found company name, fetch reviews for it
            if ((companyName != null && !companyName.isBlank()) && (reviewAnalysis == null || reviewAnalysis.getTotalReviews() == 0)) {
                try {
                    System.out.println("🔍 Fetching reviews for detected company: " + companyName);
                    reviewAnalysis = companyReviewService.fetchAndAnalyzeCompanyReviews(companyName);
                    if (aiResponse != null) {
                        aiResponse.setReviewAnalysis(reviewAnalysis);
                        aiResponse.setReviewBasedRiskAdjustment(generateReviewRiskAdjustment(aiResponse, reviewAnalysis));
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to fetch reviews for detected company: " + e.getMessage());
                }
            }
        }
        
        // Step 6.6: Set the final extracted company name in the response
        if (companyName != null && !companyName.isBlank() && aiResponse != null) {
            aiResponse.setCompanyName(companyName);
            System.out.println("✅ FINAL - Company name set in response: " + companyName);
        }

        // Step 7: Enhance AI response with URL analysis data if available
        if (urlAnalysisResponse != null) {
            enhanceResponseWithUrlAnalysis(aiResponse, urlAnalysisResponse);
        }

        return aiResponse;
    }

    /**
     * Enhances the AI analysis response with URL verification results.
     * If URL analysis reveals high risk, it can downgrade the trust score.
     */
    private void enhanceResponseWithUrlAnalysis(AiAnalysisResponseDto aiResponse, UrlAnalysisResponse urlAnalysis) {
        // ...existing code...
        if ("HIGH".equals(urlAnalysis.getRiskLevel())) {
            aiResponse.getSuspiciousIndicators().add("URL verification detected HIGH RISK: " + urlAnalysis.getAnalysisSummary());
            double urlTrustScore = urlAnalysis.getTrustScore();
            double currentTrustScore = aiResponse.getTrustScore();
            if (urlTrustScore < 3.0) {
                aiResponse.setTrustScore((currentTrustScore + urlTrustScore * 2) / 3);
            } else {
                aiResponse.setTrustScore((currentTrustScore + urlTrustScore) / 2);
            }
        }
        if (urlAnalysis.getSuggestions() != null && !urlAnalysis.getSuggestions().isEmpty()) {
            aiResponse.getSuggestions().addAll(urlAnalysis.getSuggestions());
        }
    }
    
    /**
     * Extract company name from email address.
     * Checks both the local part (before @) and domain part (after @).
     * 
     * Examples:
     * - "hr@amazon.com" → "Amazon" (from domain)
     * - "amazon-hr@gmail.com" → "Amazon" (from local part)
     * - "john.doe@microsoft.com" → "Microsoft" (from domain)
     * - "microsoft-recruiter@yahoo.com" → "Microsoft" (from local part)
     */
    private String extractCompanyFromEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        try {
            String[] parts = email.split("@");
            if (parts.length < 2) {
                return null;
            }
            
            String localPart = parts[0].toLowerCase();  // before @
            String domainPart = parts[1].toLowerCase(); // after @
            
            // Check if domain is a free email provider (gmail, yahoo, outlook, etc.)
            boolean isFreeEmailProvider = isFreeEmailProvider(domainPart);
            
            // STRATEGY 1: If domain is free email (gmail, yahoo, etc.), extract from local part
            if (isFreeEmailProvider) {
                String companyFromLocal = extractCompanyFromLocalPart(localPart);
                if (companyFromLocal != null && !companyFromLocal.isBlank()) {
                    System.out.println("📧 Extracted from email local part (free provider): " + companyFromLocal);
                    return companyFromLocal;
                }
            }
            
            // STRATEGY 2: If domain is corporate, extract from domain
            String companyFromDomain = extractCompanyFromDomain(domainPart);
            if (companyFromDomain != null && !companyFromDomain.isBlank()) {
                System.out.println("📧 Extracted from email domain: " + companyFromDomain);
                return companyFromDomain;
            }
            
            // STRATEGY 3: If domain extraction failed but local part has info, use it
            String companyFromLocal = extractCompanyFromLocalPart(localPart);
            if (companyFromLocal != null && !companyFromLocal.isBlank()) {
                System.out.println("📧 Extracted from email local part (fallback): " + companyFromLocal);
                return companyFromLocal;
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Error extracting company from email: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if the domain is a free email provider (gmail, yahoo, outlook, etc.)
     */
    private boolean isFreeEmailProvider(String domain) {
        String[] freeProviders = {
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "mail.com",
            "aol.com", "protonmail.com", "temp-mail.org", "guerrillamail.com",
            "mailinator.com", "yopmail.com", "tempmail.com", "10minutemail.com",
            "fastmail.com", "tutanota.com", "zoho.com", "rediffmail.com",
            "inbox.com", "mail.ru", "yandex.com", "qq.com"
        };
        
        for (String provider : freeProviders) {
            if (domain.equals(provider) || domain.endsWith("." + provider)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Extract company name from domain part (after @)
     * Example: "amazon.com" → "Amazon"
     */
    private String extractCompanyFromDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return null;
        }
        try {
            String[] parts = domain.split("\\.");
            if (parts.length > 0) {
                String company = parts[0].trim();
                if (company.length() > 0) {
                    // Capitalize first letter
                    company = company.substring(0, 1).toUpperCase() + 
                             (company.length() > 1 ? company.substring(1) : "");
                    return company;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
    
    /**
     * Extract company name from local part (before @)
     * Handles patterns like: "company-hr", "john.company", "company_recruiter", etc.
     * 
     * Examples:
     * - "amazon-hr" → "Amazon"
     * - "amazon_recruiter" → "Amazon"
     * - "john.amazon" → "Amazon"
     * - "recruiter-google" → "Google"
     */
    private String extractCompanyFromLocalPart(String localPart) {
        if (localPart == null || localPart.isBlank()) {
            return null;
        }
        
        // Common prefixes and suffixes in email local parts
        String[] companyIndicators = { "hr", "recruiter", "recruit", "hiring", "careers", 
                                       "jobs", "apply", "interview", "candidate", "contact" };
        
        // Split by common delimiters: -, _, .
        String[] words = localPart.split("[\\-_.]");
        
        // Filter out common non-company words
        for (String word : words) {
            if (word.length() > 2 && !isCommonWord(word) && !isStopWord(word)) {
                // Found a meaningful word that could be company name
                // Capitalize first letter
                String company = word.substring(0, 1).toUpperCase() + 
                                (word.length() > 1 ? word.substring(1) : "");
                return company;
            }
        }
        
        return null;
    }
    
    /**
     * Extract company name from URL domain
     */
    private String extractCompanyFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            String domain = url.replaceAll("(?i)^https?://", "").replaceAll("(?i)^www\\.", "");
            String[] parts = domain.split("\\.");
            if (parts.length > 0) {
                String company = parts[0];
                if (company.equalsIgnoreCase("careers") || company.equalsIgnoreCase("jobs") 
                    || company.equalsIgnoreCase("apply") || company.equalsIgnoreCase("recruit")) {
                    if (parts.length > 1) {
                        company = parts[1];
                    }
                }
                company = company.substring(0, 1).toUpperCase() + company.substring(1);
                System.out.println("🌐 Extracted from URL domain: " + company);
                return company;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
    
    /**
     * Extract company name from AI's companyDetails field by identifying key company identifiers
     */
    private String extractCompanyNameFromDetails(String companyDetails) {
        if (companyDetails == null || companyDetails.isBlank()) {
            return null;
        }
        try {
            // Extract all capitalized words/phrases that could be company names
            java.util.regex.Pattern companyPattern = java.util.regex.Pattern.compile("\\b([A-Z][A-Za-z0-9&.,\\-]*(?:\\s+[A-Z][A-Za-z0-9&.,\\-]*)*?)\\b");
            java.util.regex.Matcher matcher = companyPattern.matcher(companyDetails);
            
            java.util.List<String> candidates = new java.util.ArrayList<>();
            while (matcher.find()) {
                String candidate = matcher.group(1).trim();
                // Filter out common words and very short names
                if (candidate.length() > 2 && !isCommonWord(candidate) && !isStopWord(candidate)) {
                    candidates.add(candidate);
                }
            }
            
            // Return the first meaningful company name candidate
            if (!candidates.isEmpty()) {
                String company = candidates.get(0);
                System.out.println("📋 Extracted from companyDetails: " + company);
                return company;
            }
        } catch (Exception e) {
            System.err.println("Error extracting company from details: " + e.getMessage());
            return null;
        }
        return null;
    }
    
    /**
     * Compare company names from multiple sources (AI, Email, URL) and find the best match.
     * Returns the company name that appears in multiple sources or the most reliable single source.
     */
    private String findBestCompanyName(String aiCompanyName, String emailCompanyName, String urlCompanyName) {
        System.out.println("\n🔍 Comparing company names from all sources:");
        System.out.println("   AI Response: " + (aiCompanyName != null ? aiCompanyName : "null"));
        System.out.println("   Email Domain: " + (emailCompanyName != null ? emailCompanyName : "null"));
        System.out.println("   URL Domain: " + (urlCompanyName != null ? urlCompanyName : "null"));
        
        java.util.List<String> sources = new java.util.ArrayList<>();
        if (aiCompanyName != null && !aiCompanyName.isBlank()) sources.add(aiCompanyName);
        if (emailCompanyName != null && !emailCompanyName.isBlank()) sources.add(emailCompanyName);
        if (urlCompanyName != null && !urlCompanyName.isBlank()) sources.add(urlCompanyName);
        
        if (sources.isEmpty()) {
            return null;
        }
        
        // If only one source, return it
        if (sources.size() == 1) {
            System.out.println("✅ Single source match: " + sources.get(0));
            return sources.get(0);
        }
        
        // Check for exact matches across sources
        for (String candidate : sources) {
            int matchCount = 0;
            for (String source : sources) {
                if (source.equalsIgnoreCase(candidate)) {
                    matchCount++;
                }
            }
            if (matchCount >= 2) {
                System.out.println("✅ Exact match found in " + matchCount + " sources: " + candidate);
                return candidate;
            }
        }
        
        // Check for partial/substring matches (one contains the other)
        for (int i = 0; i < sources.size(); i++) {
            for (int j = i + 1; j < sources.size(); j++) {
                String source1 = sources.get(i).toLowerCase();
                String source2 = sources.get(j).toLowerCase();
                
                // Check if one is contained in the other
                if (source1.contains(source2) || source2.contains(source1)) {
                    String shorter = source1.length() < source2.length() ? sources.get(i) : sources.get(j);
                    System.out.println("✅ Partial match found: " + shorter);
                    return shorter;
                }
                
                // Check for common keywords
                String[] words1 = source1.split("\\s+");
                String[] words2 = source2.split("\\s+");
                
                for (String word1 : words1) {
                    for (String word2 : words2) {
                        if (word1.equals(word2) && word1.length() > 2) {
                            System.out.println("✅ Common keyword found: " + word1);
                            return word1;
                        }
                    }
                }
            }
        }
        
        // If no match found, prioritize: AI > Email > URL
        System.out.println("⚠️ No exact match. Using highest priority source.");
        if (aiCompanyName != null && !aiCompanyName.isBlank()) {
            System.out.println("   Using AI response: " + aiCompanyName);
            return aiCompanyName;
        }
        if (emailCompanyName != null && !emailCompanyName.isBlank()) {
            System.out.println("   Using email domain: " + emailCompanyName);
            return emailCompanyName;
        }
        return null;
    }
    
    /**
     * Check if a word is a common English word (not a company name)
     */
    private boolean isCommonWord(String word) {
        String[] commonWords = {"The", "This", "That", "Join", "We", "Our", "Your", 
                              "Apply", "Contact", "Email", "Send", "Please", "Dear",
                              "Subject", "Team", "Company", "Job", "Position", "Hiring",
                              "For", "At", "In", "Inc", "Ltd", "LLC", "Pvt", "Corp",
                              "Is", "Are", "Be", "Have", "Has", "Was", "Were", "Do",
                              "Does", "Did", "Will", "Would", "Should", "Could", "Can"};
        for (String common : commonWords) {
            if (word.equalsIgnoreCase(common)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a word is a stop word
     */
    private boolean isStopWord(String word) {
        String[] stopWords = {"And", "Or", "But", "Not", "No", "Yes", "All", "Each", "Every",
                             "Some", "Any", "About", "From", "To", "By", "As", "Into", "With"};
        for (String stop : stopWords) {
            if (word.equalsIgnoreCase(stop)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Generate explanation of how reviews affected risk assessment
     */
    private String generateReviewRiskAdjustment(AiAnalysisResponseDto aiResponse, ReviewAnalysis reviewAnalysis) {
        StringBuilder adjustment = new StringBuilder();
        if (reviewAnalysis == null || reviewAnalysis.getTotalReviews() == 0) {
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
}