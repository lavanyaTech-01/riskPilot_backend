package com.riskpilot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for URL verification and risk analysis.
 * Users submit a URL to be analyzed for scam indicators and security risks.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UrlAnalysisRequest {

    /**
     * The URL to be analyzed (e.g., https://xyztech-careers-job.com)
     */
    private String url;

    /**
     * Optional: Known legitimate company domain for typosquatting detection
     * (e.g., amazon.com for comparison with amaz0n-careers.com)
     */
    private String knownCompanyDomain;

    /**
     * Optional: Email associated with the job offer for cross-verification
     */
    private String senderEmail;

    /**
     * Optional: Company name mentioned in the job offer
     */
    private String companyName;
}
