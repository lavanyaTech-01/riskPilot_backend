package com.riskpilot.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for email analysis results.
 * Contains comprehensive risk assessment including domain validation,
 * lookalike detection, free provider detection, and calculated risk scores.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailAnalysisResponse {

    /**
     * Overall risk level: LOW, MEDIUM, or HIGH
     */
    private String riskLevel;

    /**
     * Trust score on 0-100 scale
     * 0 = Highly suspicious (very likely a scam)
     * 50 = Medium risk/neutral
     * 100 = Highly trustworthy (likely legitimate)
     */
    private Integer trustScore;

    /**
     * Calculated risk score (0-100) used for internal scoring
     * 0 = No risk, 100 = Maximum risk
     */
    private Integer riskScore;

    /**
     * The email address analyzed
     */
    private String email;

    /**
     * The email domain extracted from the email address
     */
    private String emailDomain;

    /**
     * Whether the email domain is a free email provider
     * (gmail.com, yahoo.com, outlook.com, etc.)
     */
    private Boolean isFreeEmailProvider;

    /**
     * Type of free email provider if applicable
     * Examples: Gmail, Yahoo, Outlook
     */
    private String freeEmailProviderType;

    /**
     * Classification of email domain: TRUSTED, UNKNOWN, or SUSPICIOUS
     * TRUSTED: Well-known verified domains (e.g., major companies)
     * UNKNOWN: Custom domains with no strong reputation
     * SUSPICIOUS: Lookalike domains, misleading names, or scam patterns
     */
    private String domainType;

    /**
     * Whether the email domain is a lookalike of known phishing patterns
     * Example: tcs-career-job.com (suspicious domain)
     */
    private Boolean isLookalikeDomain;

    /**
     * Suspicious score indicating how much the domain looks like a phishing domain (0-100)
     */
    private Integer phishingSimilarityScore;

    /**
     * List of suspicious keywords detected in the email domain
     * Examples: "jobs", "hiring", "recruitment", "verify", "urgent"
     */
    private List<String> domainSuspiciousIndicators;

    /**
     * List of all suspicious indicators found during analysis
     */
    private List<String> suspiciousIndicators;

    /**
     * List of recommendations for the user
     */
    private List<String> suggestions;

    /**
     * Overall analysis summary
     */
    private String analysisSummary;

    /**
     * Confidence level of the analysis (LOW, MEDIUM, HIGH)
     * Based on domain analysis reliability
     */
    private String confidenceLevel;
}
