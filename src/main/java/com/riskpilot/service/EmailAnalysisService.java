package com.riskpilot.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.riskpilot.model.EmailAnalysisRequest;
import com.riskpilot.model.EmailAnalysisResponse;
import com.riskpilot.service.constants.EmailAnalysisConstants;

/**
 * Comprehensive Email Analysis Service - FIXED VERSION
 * 
 * Correctly separates:
 * - Domain-level analysis (classification, reputation, phishing)
 * - Username-level analysis (pattern, professionalism)
 * - Accurate indicator generation
 * - Consistent risk/trust scoring
 */
@Service
public class EmailAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(EmailAnalysisService.class);
    private final Map<String, EmailAnalysisResponse> analysisCache = new HashMap<>();

    /**
     * Main email analysis method
     */
    public EmailAnalysisResponse analyzeEmail(EmailAnalysisRequest request) {
        EmailAnalysisResponse response = new EmailAnalysisResponse();
        List<String> indicators = new ArrayList<>();
        int riskScore = 0;

        try {
            // Validate request
            if (request == null || request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                throw new IllegalArgumentException("Email is required");
            }

            String email = request.getEmail().toLowerCase().trim();
            
            // Parse email into username and domain
            EmailParts parts = parseEmail(email);
            if (parts == null) {
                throw new IllegalArgumentException("Invalid email format");
            }

            response.setEmail(email);
            response.setEmailDomain(parts.domain);

            // ===== DOMAIN ANALYSIS =====
            String domainType = classifyDomain(parts.domain);
            response.setDomainType(domainType);
            
            // Set free email provider flag
            boolean isFreeProvider = isFreeEmailProvider(parts.domain);
            response.setIsFreeEmailProvider(isFreeProvider);
            if (isFreeProvider) {
                response.setFreeEmailProviderType(getFreeEmailProviderType(parts.domain));
            }
            
            // Set phishing score and lookalike detection
            int phishingScore = detectPhishingPatterns(parts.domain);
            response.setPhishingSimilarityScore(phishingScore);
            response.setIsLookalikeDomain(phishingScore >= 70);
            
            List<String> domainIndicators = analyzeDomain(parts.domain, domainType);
            indicators.addAll(domainIndicators);
            riskScore += calculateDomainRisk(domainType, parts.domain);

            // ===== USERNAME ANALYSIS =====
            List<String> usernameIndicators = analyzeUsername(parts.username);
            indicators.addAll(usernameIndicators);
            riskScore += calculateUsernameRisk(parts.username);

            // ===== FINAL SCORING =====
            riskScore = Math.max(0, Math.min(100, riskScore));
            response.setRiskScore(riskScore);
            response.setSuspiciousIndicators(indicators);

            // Convert to trust score (inverse of risk)
            int trustScore = 100 - riskScore;
            response.setTrustScore(trustScore);

            // Determine risk level (consistent with scores)
            String riskLevel = determineRiskLevel(riskScore);
            response.setRiskLevel(riskLevel);

            // Calculate confidence
            String confidence = calculateConfidence(domainType, riskScore);
            response.setConfidenceLevel(confidence);

            // Generate optimized suggestions (2-3 only)
            List<String> suggestions = generateOptimizedSuggestions(response);
            response.setSuggestions(suggestions);

            // Generate summary
            String summary = generateAnalysisSummary(response);
            response.setAnalysisSummary(summary);

            logger.info("Email analyzed - Risk: {}, Level: {}, Domain: {}", riskScore, riskLevel, domainType);
            return response;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error analyzing email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to analyze email: " + e.getMessage(), e);
        }
    }

    /**
     * Parse email into username and domain
     */
    private EmailParts parseEmail(String email) {
        if (!email.contains("@")) {
            return null;
        }
        
        String[] parts = email.split("@");
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            return null;
        }
        
        return new EmailParts(parts[0], parts[1]);
    }

    /**
     * Analyze DOMAIN ONLY - never domain keywords from username
     */
    private List<String> analyzeDomain(String domain, String domainType) {
        List<String> indicators = new ArrayList<>();

        // Free provider indicator
        if (isFreeEmailProvider(domain)) {
            indicators.add("Email from free provider: " + getFreeEmailProviderType(domain));
        } else if ("TRUSTED".equals(domainType)) {
            // Only add if NOT free provider AND is trusted
            indicators.add("Email from verified company domain");
        } else if ("UNKNOWN".equals(domainType)) {
            // Only add if NOT free provider AND is unknown
            indicators.add("Domain is not associated with a known or verified organization");
        } else if ("SUSPICIOUS".equals(domainType)) {
            // Only add if NOT free provider AND is suspicious
            int phishingScore = detectPhishingPatterns(domain);
            if (phishingScore >= 70) {
                indicators.add("Suspicious domain pattern detected (score: " + phishingScore + "%)");
            }
        }

        return indicators;
    }

    /**
     * Analyze USERNAME ONLY - patterns, professionalism, randomness
     */
    private List<String> analyzeUsername(String username) {
        List<String> indicators = new ArrayList<>();

        // Check for random numbers
        if (hasRandomNumericSuffix(username)) {
            indicators.add("Non-professional email naming: random numeric suffix detected");
        }

        // Check for generic recruiter names with numbers
        if (isGenericRecruiterPattern(username)) {
            indicators.add("Generic recruiter username pattern detected");
        }

        // Check for suspicious patterns
        if (hasSuspiciousPattern(username)) {
            indicators.add("Suspicious username pattern: " + username);
        }

        return indicators;
    }

    /**
     * Check if username has random numeric suffix (e.g., hr12345)
     */
    private boolean hasRandomNumericSuffix(String username) {
        // Pattern: text followed by 3+ random numbers
        Pattern pattern = Pattern.compile("^[a-z]{2,}\\d{3,}$");
        return pattern.matcher(username).matches();
    }

    /**
     * Check for generic recruiter patterns
     */
    private boolean isGenericRecruiterPattern(String username) {
        String[] genericPatterns = {"hr", "jobs", "hiring", "recruit", "careers"};
        
        for (String pattern : genericPatterns) {
            if (username.equals(pattern) || username.matches("^" + pattern + "\\d+$")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check for other suspicious username patterns
     */
    private boolean hasSuspiciousPattern(String username) {
        // All numbers or mostly numbers
        if (username.matches("\\d{5,}")) {
            return true;
        }
        
        // Too many dots or special chars
        if (username.replaceAll("[a-z0-9]", "").length() > 3) {
            return true;
        }
        
        return false;
    }

    /**
     * Classify domain (NEVER classify based on username)
     */
    private String classifyDomain(String domain) {
        // Step 1: Check if free provider
        if (isFreeEmailProvider(domain)) {
            return "FREE_PROVIDER";
        }

        // Step 2: Check if trusted
        if (isTrustedDomain(domain)) {
            return "TRUSTED";
        }

        // Step 3: Check for phishing/lookalike
        int phishingScore = detectPhishingPatterns(domain);
        if (phishingScore >= 70) {
            return "SUSPICIOUS";
        }

        // Step 4: Default to unknown
        return "UNKNOWN";
    }

    /**
     * Calculate risk from DOMAIN ONLY
     */
    private int calculateDomainRisk(String domainType, String domain) {
        int risk = 0;

        switch (domainType) {
            case "FREE_PROVIDER":
                risk += 20; // Free provider moderate risk
                break;
            case "TRUSTED":
                risk -= 20; // Trusted domain bonus
                break;
            case "UNKNOWN":
                risk += 25; // Unknown domain medium risk
                break;
            case "SUSPICIOUS":
                risk += 35; // Suspicious domain high risk
                break;
        }

        // Phishing pattern scoring
        int phishingScore = detectPhishingPatterns(domain);
        if (phishingScore >= 70) {
            risk += 20; // Additional phishing risk
        }

        return risk;
    }

    /**
     * Calculate risk from USERNAME ONLY
     */
    private int calculateUsernameRisk(String username) {
        int risk = 0;

        if (hasRandomNumericSuffix(username)) {
            risk += 10;
        }

        if (isGenericRecruiterPattern(username)) {
            risk += 15;
        }

        if (hasSuspiciousPattern(username)) {
            risk += 8;
        }

        return risk;
    }

    /**
     * Determine risk level (consistent with scores)
     */
    private String determineRiskLevel(int riskScore) {
        if (riskScore <= 30) {
            return "LOW";
        } else if (riskScore <= 60) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }

    /**
     * Calculate confidence based on evidence
     */
    private String calculateConfidence(String domainType, int riskScore) {
        // HIGH confidence: clear evidence
        if ("TRUSTED".equals(domainType) || riskScore >= 70) {
            return "HIGH";
        }

        // LOW confidence: no clear signals
        if ("UNKNOWN".equals(domainType) && riskScore < 40) {
            return "LOW";
        }

        // MEDIUM: partial signals
        return "MEDIUM";
    }

    /**
     * Generate 2-3 optimized suggestions (avoid repetition)
     */
    private List<String> generateOptimizedSuggestions(EmailAnalysisResponse response) {
        List<String> suggestions = new ArrayList<>();

        if ("LOW".equals(response.getRiskLevel())) {
            suggestions.add("Email appears legitimate. Verify through official company website for sensitive matters.");
            return suggestions;
        }

        if ("HIGH".equals(response.getRiskLevel())) {
            suggestions.add("This appears to be a phishing/scam attempt. Do not click links or share information.");
            suggestions.add("Report to your email provider and the organization being impersonated.");
            return suggestions;
        }

        // MEDIUM risk
        suggestions.add("Verify recruiter via official company website or LinkedIn.");
        suggestions.add("Avoid sharing sensitive information without independent verification.");
        if (response.getIsFreeEmailProvider() != null && response.getIsFreeEmailProvider()) {
            suggestions.add("Note: Professional recruiters rarely use free email providers.");
        }

        return suggestions.size() > 3 ? suggestions.subList(0, 3) : suggestions;
    }

    /**
     * Generate analysis summary
     */
    private String generateAnalysisSummary(EmailAnalysisResponse response) {
        StringBuilder summary = new StringBuilder();
        summary.append("Email Security Analysis\n");
        summary.append("=".repeat(40)).append("\n");
        summary.append("Email: ").append(response.getEmail()).append("\n");
        summary.append("Risk: ").append(response.getRiskLevel());
        summary.append(" (Score: ").append(response.getRiskScore()).append("/100)\n");
        summary.append("Trust: ").append(response.getTrustScore()).append("/100\n");
        summary.append("Domain Type: ").append(response.getDomainType()).append("\n");
        summary.append("\nKey Findings:\n");
        
        if (response.getSuspiciousIndicators() != null && !response.getSuspiciousIndicators().isEmpty()) {
            for (String indicator : response.getSuspiciousIndicators()) {
                summary.append("• ").append(indicator).append("\n");
            }
        }
        
        return summary.toString();
    }

    /**
     * ===== DOMAIN ANALYSIS METHODS =====
     */

    private boolean isFreeEmailProvider(String domain) {
        return EmailAnalysisConstants.FREE_EMAIL_PROVIDERS.contains(domain);
    }

    private String getFreeEmailProviderType(String domain) {
        return switch (domain) {
            case "gmail.com" -> "Gmail";
            case "yahoo.com" -> "Yahoo";
            case "outlook.com", "hotmail.com" -> "Outlook";
            case "aol.com" -> "AOL";
            case "protonmail.com" -> "ProtonMail";
            case "tutanota.com" -> "Tutanota";
            case "yandex.com", "mail.ru" -> "Yandex";
            default -> "Free Provider";
        };
    }

    private boolean isTrustedDomain(String domain) {
        String normalized = domain.toLowerCase();
        if (normalized.startsWith("www.")) {
            normalized = normalized.substring(4);
        }

        if (EmailAnalysisConstants.TRUSTED_DOMAINS.contains(normalized)) {
            return true;
        }

        String[] parts = normalized.split("\\.");
        if (parts.length > 2) {
            String parent = parts[parts.length - 2] + "." + parts[parts.length - 1];
            return EmailAnalysisConstants.TRUSTED_DOMAINS.contains(parent);
        }

        return false;
    }

    private int detectPhishingPatterns(String domain) {
        int score = 0;
        String name = domain.toLowerCase().split("\\.")[0];

        // Suspicious keywords
        for (String keyword : EmailAnalysisConstants.SUSPICIOUS_EMAIL_KEYWORDS) {
            if (name.contains(keyword)) {
                score += 15;
            }
        }

        // Multiple hyphens
        int hyphens = (int) name.chars().filter(ch -> ch == '-').count();
        score += (hyphens * 12);

        // Similarity to known domains
        for (String known : EmailAnalysisConstants.KNOWN_COMPANY_DOMAINS) {
            String knownName = known.split("\\.")[0];
            int sim = calculateSimilarity(name.replaceAll("-", ""), knownName.replaceAll("-", ""));
            if (sim >= 60 && sim < 95) {
                score += (100 - sim);
            }
        }

        return Math.min(100, score);
    }

    private int calculateSimilarity(String s1, String s2) {
        int distance = levenshteinDistance(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        return maxLen == 0 ? 100 : Math.round(100 * (maxLen - distance) / (float) maxLen);
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dist = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) dist[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dist[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dist[i][j] = Math.min(
                    Math.min(dist[i - 1][j] + 1, dist[i][j - 1] + 1),
                    dist[i - 1][j - 1] + cost
                );
            }
        }

        return dist[s1.length()][s2.length()];
    }

    /**
     * ===== UTILITY METHODS =====
     */

    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public List<String> extractEmailsFromText(String text) {
        List<String> emails = new ArrayList<>();
        if (text == null) return emails;
        
        Pattern pattern = Pattern.compile("[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            emails.add(matcher.group());
        }
        return emails;
    }

    public List<EmailAnalysisResponse> analyzeEmailBatch(List<String> emails) {
        List<EmailAnalysisResponse> results = new ArrayList<>();
        for (String email : emails) {
            EmailAnalysisRequest req = new EmailAnalysisRequest();
            req.setEmail(email);
            results.add(analyzeEmail(req));
        }
        return results;
    }

    public EmailAnalysisResponse analyzeEmailWithCache(EmailAnalysisRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        
        if (analysisCache.containsKey(email)) {
            logger.debug("Cache hit: {}", email);
            return analysisCache.get(email);
        }
        
        EmailAnalysisResponse response = analyzeEmail(request);
        analysisCache.put(email, response);
        return response;
    }

    public void clearCache() {
        analysisCache.clear();
    }

    public Map<String, Object> getSenderProfile(String email) {
        EmailAnalysisRequest req = new EmailAnalysisRequest();
        req.setEmail(email);
        EmailAnalysisResponse resp = analyzeEmail(req);
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("email", email);
        profile.put("riskLevel", resp.getRiskLevel());
        profile.put("riskScore", resp.getRiskScore());
        profile.put("trustScore", resp.getTrustScore());
        profile.put("domainType", resp.getDomainType());
        profile.put("isProfessional", "TRUSTED".equals(resp.getDomainType()));
        profile.put("isPhishing", "HIGH".equals(resp.getRiskLevel()));
        
        return profile;
    }

    public Map<String, List<String>> categorizeEmailsByRisk(List<String> emails) {
        Map<String, List<String>> result = new HashMap<>();
        result.put("LOW", new ArrayList<>());
        result.put("MEDIUM", new ArrayList<>());
        result.put("HIGH", new ArrayList<>());
        
        for (String email : emails) {
            EmailAnalysisRequest req = new EmailAnalysisRequest();
            req.setEmail(email);
            EmailAnalysisResponse resp = analyzeEmail(req);
            result.get(resp.getRiskLevel()).add(email);
        }
        
        return result;
    }

    public Map<String, Object> generateSecurityReport(List<String> emails) {
        int low = 0, medium = 0, high = 0;
        double avgRisk = 0;
        
        for (String email : emails) {
            EmailAnalysisRequest req = new EmailAnalysisRequest();
            req.setEmail(email);
            EmailAnalysisResponse resp = analyzeEmail(req);
            
            switch (resp.getRiskLevel()) {
                case "LOW" -> low++;
                case "MEDIUM" -> medium++;
                case "HIGH" -> high++;
            }
            avgRisk += resp.getRiskScore();
        }
        
        avgRisk = emails.size() > 0 ? avgRisk / emails.size() : 0;
        
        Map<String, Object> report = new HashMap<>();
        report.put("totalAnalyzed", emails.size());
        report.put("lowRisk", low);
        report.put("mediumRisk", medium);
        report.put("highRisk", high);
        report.put("averageRiskScore", Math.round(avgRisk * 100.0) / 100.0);
        report.put("threatPercentage", emails.size() > 0 ? Math.round((double)(medium + high) / emails.size() * 100) : 0);
        
        return report;
    }

    /**
     * Extract company name from email domain
     * Removes common TLDs and returns the company name portion
     * e.g., "amazon.com" -> "Amazon", "xyz-careers.com" -> "Xyz-Careers"
     */
    public static String extractCompanyNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        
        try {
            String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
            // Remove TLD (.com, .co.uk, etc.)
            String[] parts = domain.split("\\.");
            if (parts.length > 0) {
                // Capitalize first letter of domain name
                String companyName = parts[0];
                return companyName.substring(0, 1).toUpperCase() + companyName.substring(1);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * Inner class for email parts
     */
    private static class EmailParts {
        String username;
        String domain;

        EmailParts(String username, String domain) {
            this.username = username;
            this.domain = domain;
        }
    }
}
