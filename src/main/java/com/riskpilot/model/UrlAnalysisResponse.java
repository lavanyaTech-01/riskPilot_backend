package com.riskpilot.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for URL analysis results.
 * Contains comprehensive risk assessment data including domain age, SSL status,
 * redirects, malware detection, and calculated risk scores.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UrlAnalysisResponse {

    /**
     * Overall risk level: LOW, MEDIUM, or HIGH
     */
    private String riskLevel;

    /**
     * Trust score on 0-10 scale
     * 0 = Highly suspicious (very likely a scam)
     * 5 = Medium risk
     * 10 = Highly trustworthy (likely legitimate)
     */
    private double trustScore;

    /**
     * Domain age in days
     */
    private Integer domainAgeDays;

    /**
     * Domain creation date (if available)
     */
    private String domainCreationDate;

    /**
     * Domain registrar information
     */
    private String domainRegistrar;

    /**
     * Whether HTTPS is used and SSL certificate is valid
     */
    private Boolean sslValid;

    /**
     * Details about SSL certificate status
     */
    private String sslDetails;

    /**
     * Whether the domain is flagged by VirusTotal or malware databases
     */
    private Boolean malwareFlagged;

    /**
     * Number of security engines that flagged the domain
     */
    private Integer malwareEngineCount;

    /**
     * List of security engines that flagged the domain
     */
    private List<String> flaggedSecurityEngines;

    /**
     * Whether typosquatting was detected (look-alike domain detected)
     */
    private Boolean typosquattingDetected;

    /**
     * Similarity score with known company domain (0-100)
     */
    private Integer typosquattingSimilarityScore;

    /**
     * List of suspicious indicators found during analysis
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
     * Raw domain name extracted from the URL
     */
    private String domain;

    /**
     * Whether domain has a valid DNS record
     */
    private Boolean dnsResolvable;

    /**
     * Server IP address (if resolved)
     */
    private String serverIp;

    /**
     * Calculated risk score (0-100) used for internal scoring
     */
    private Integer riskScore;

    /**
     * Final URL after all redirects are followed
     * Ensures we know the actual destination, not just the initial URL
     */
    private String finalRedirectUrl;

    /**
     * Whether a redirect was explicitly detected during analysis
     */
    private Boolean redirectDetected;

    /**
     * Target company/domain detected via redirects
     * Extracted from the final URL after all redirects are resolved
     */
    private String targetCompany;

    /**
     * Details about the target detected via redirects (e.g., "Target detected via HTTP redirect")
     */
    private String targetAnalysisDetails;
}

