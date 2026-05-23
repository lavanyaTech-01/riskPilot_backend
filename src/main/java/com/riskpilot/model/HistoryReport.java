package com.riskpilot.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity to store history of all scan analyses (URL, Email, File, Description)
 * Maintains a common record structure with nullable fields for different analysis types
 */
@Entity
@Table(name = "history_report")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistoryReport {
	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    // ===== SCAN INPUT FIELDS (Common) =====
	    /**
	     * Type of scan: URL, EMAIL, FILE, DESCRIPTION
	     */
	    @Column(name = "scan_type", nullable = true)
	    private String scanType;

	    /**
	     * URL being analyzed (nullable - only for URL scans)
	     */
	    @Column(name = "url", nullable = true, length = 500)
	    private String url;

	    /**
	     * Email being analyzed (nullable - for EMAIL or DESCRIPTION scans)
	     */
	    @Column(name = "email", nullable = true, length = 255)
	    private String email;

	    /**
	     * File name (nullable - only for FILE scans)
	     */
	    @Column(name = "file_name", nullable = true, length = 255)
	    private String fileName;

	    /**
	     * File type/extension (nullable - only for FILE scans)
	     */
	    @Column(name = "file_type", nullable = true, length = 100)
	    private String fileType;

	    /**
	     * File content as bytes (nullable - only for FILE scans)
	     */
	    @Lob
	    @Column(name = "file_data", nullable = true)
	    private byte[] fileData;

	    /**
	     * Job description text (nullable - only for DESCRIPTION scans)
	     */
	    @Lob
	    @Column(name = "job_description", nullable = true)
	    private String jobDescription;

	    /**
	     * Company name extracted or provided (nullable - common field)
	     */
	    @Column(name = "company_name", nullable = true, length = 255)
	    private String companyName;

	    // ===== ANALYSIS RESULT FIELDS (Common) =====
	    /**
	     * Overall risk level: LOW, MEDIUM, HIGH (nullable)
	     */
	    @Column(name = "risk_level", nullable = true, length = 50)
	    private String riskLevel;

	    /**
	     * Trust score (scale varies: 0-10 for URL, 0-100 for EMAIL)
	     */
	    @Column(name = "trust_score", nullable = true)
	    private Double trustScore;

	    /**
	     * Risk score 0-100 (nullable - varies by analysis type)
	     */
	    @Column(name = "risk_score", nullable = true)
	    private Integer riskScore;

	    /**
	     * Email verified flag (nullable)
	     */
	    @Column(name = "email_verified", nullable = true)
	    private Boolean emailVerified;

	    /**
	     * Company verified flag (nullable)
	     */
	    @Column(name = "company_verified", nullable = true)
	    private Boolean companyVerified;

	    /**
	     * Company details (nullable)
	     */
	    @Column(name = "company_details", nullable = true, length = 1000)
	    private String companyDetails;

	    /**
	     * Analysis summary (nullable)
	     */
	    @Column(name = "analysis_summary", nullable = true, columnDefinition = "TEXT")
	    private String analysisSummary;

	    // ===== DETAILED RESPONSE STORAGE (as JSON strings) =====
	    /**
	     * Complete URL analysis response stored as JSON (nullable)
	     * Stores UrlAnalysisResponse object serialized to JSON
	     */
	    @Lob
	    @Column(name = "url_analysis_response", nullable = true)
	    private String urlAnalysisResponse;

	    /**
	     * Complete email analysis response stored as JSON (nullable)
	     * Stores EmailAnalysisResponse object serialized to JSON
	     */
	    @Lob
	    @Column(name = "email_analysis_response", nullable = true)
	    private String emailAnalysisResponse;

	    /**
	     * Complete AI analysis response stored as JSON (nullable)
	     * Stores AiAnalysisResponseDto object serialized to JSON (for FILE and DESCRIPTION scans)
	     */
	    @Lob
	    @Column(name = "ai_analysis_response", nullable = true)
	    private String aiAnalysisResponse;

	    // ===== INDICATORS AND SUGGESTIONS =====
	    @ElementCollection(fetch = FetchType.EAGER)
	    @CollectionTable(name = "history_suspicious_indicators", joinColumns = @JoinColumn(name = "history_report_id"))
	    @Column(name = "indicator")
	    private List<String> suspiciousIndicators;

	    @ElementCollection(fetch = FetchType.EAGER)
	    @CollectionTable(name = "history_suggestions", joinColumns = @JoinColumn(name = "history_report_id"))
	    @Column(name = "suggestion")
	    private List<String> suggestions;

	    // ===== METADATA =====
	    /**
	     * Timestamp when the scan was performed
	     */
	    @Column(name = "created_at", nullable = false)
	    private LocalDateTime createdAt;

	    /**
	     * Timestamp of last update
	     */
	    @Column(name = "updated_at", nullable = true)
	    private LocalDateTime updatedAt;

	    /**
	     * Confidence level of analysis (LOW, MEDIUM, HIGH)
	     */
	    @Column(name = "confidence_level", nullable = true, length = 50)
	    private String confidenceLevel;
	    
	    @ManyToOne
	    @JoinColumn(name = "user_id", nullable = false)
	    @JsonIgnore
	    private UserCred user;
}