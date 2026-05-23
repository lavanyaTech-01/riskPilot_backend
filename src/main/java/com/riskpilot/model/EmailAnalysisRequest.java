package com.riskpilot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for email analysis and scam detection.
 * Users submit only the recruiter email address for analysis.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailAnalysisRequest {

    /**
     * The email address of the recruiter/sender
     * Example: hr@xyz-careers-job.com
     * Required: Yes
     */
    private String email;
}
