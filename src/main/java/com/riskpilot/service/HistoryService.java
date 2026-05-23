package com.riskpilot.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskpilot.model.AiAnalysisResponseDto;
import com.riskpilot.model.EmailAnalysisResponse;
import com.riskpilot.model.HistoryReport;
import com.riskpilot.model.UrlAnalysisResponse;
import com.riskpilot.model.UserCred;
import com.riskpilot.repository.HistoryRepository;

/**
 * Service to manage scan history records
 * Saves and retrieves analysis responses from all scan types (URL, EMAIL, FILE, DESCRIPTION)
 * Stores complete response objects as JSON strings for flexibility
 */
@Service
public class HistoryService {

    @Autowired
    private HistoryRepository historyRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Save URL analysis response to history
     */
    public HistoryReport saveUrlAnalysis(String url, UrlAnalysisResponse response, UserCred user) {
        try {
            HistoryReport history = new HistoryReport();
            history.setScanType("URL");
            history.setUrl(url);
            history.setUser(user);
            history.setCreatedAt(LocalDateTime.now());
            history.setUpdatedAt(LocalDateTime.now());

            if (response != null) {
                history.setRiskLevel(response.getRiskLevel());
                history.setTrustScore(response.getTrustScore());
                history.setAnalysisSummary(response.getAnalysisSummary());
                history.setSuspiciousIndicators(response.getSuspiciousIndicators());
                history.setSuggestions(response.getSuggestions());
                history.setRiskScore(response.getRiskScore());

                String responseJson = objectMapper.writeValueAsString(response);
                history.setUrlAnalysisResponse(responseJson);
            }

            return historyRepository.save(history);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save URL analysis history: " + e.getMessage(), e);
        }
    }

    /**
     * Save email analysis response to history
     */
    public HistoryReport saveEmailAnalysis(String email, EmailAnalysisResponse response, UserCred user) {
        try {
            HistoryReport history = new HistoryReport();
            history.setScanType("EMAIL");
            history.setEmail(email);
            history.setUser(user);
            history.setCreatedAt(LocalDateTime.now());
            history.setUpdatedAt(LocalDateTime.now());

            if (response != null) {
                history.setRiskLevel(response.getRiskLevel());
                history.setTrustScore(response.getTrustScore() != null ? response.getTrustScore().doubleValue() : null);
                history.setRiskScore(response.getRiskScore());
                history.setAnalysisSummary(response.getAnalysisSummary());
                history.setSuspiciousIndicators(response.getSuspiciousIndicators());
                history.setSuggestions(response.getSuggestions());

                String responseJson = objectMapper.writeValueAsString(response);
                history.setEmailAnalysisResponse(responseJson);
            }

            return historyRepository.save(history);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save email analysis history: " + e.getMessage(), e);
        }
    }

    /**
     * Save file analysis response to history (AI Analysis)
     */
    public HistoryReport saveFileAnalysis(String fileName, String fileType, byte[] fileData, 
                                          AiAnalysisResponseDto response, UserCred user) {
        try {
            HistoryReport history = new HistoryReport();
            history.setScanType("FILE");
            history.setFileName(fileName);
            history.setFileType(fileType);
            history.setFileData(fileData);
            history.setUser(user);
            history.setCreatedAt(LocalDateTime.now());
            history.setUpdatedAt(LocalDateTime.now());

            if (response != null) {
                history.setRiskLevel(response.getRiskLevel());
                history.setTrustScore(response.getTrustScore());
                history.setEmail(response.getEmail());
                history.setUrl(response.getUrl());
                history.setCompanyName(response.getCompanyName());
                history.setCompanyDetails(response.getCompanyDetails());
                history.setEmailVerified(response.getEmailVerified());
                history.setCompanyVerified(response.getCompanyVerified());
                history.setAnalysisSummary(response.getAnalysisSummary());
                // Truncate indicators to fit VARCHAR(255) column limit
                history.setSuspiciousIndicators(truncateIndicators(response.getSuspiciousIndicators()));
                history.setSuggestions(truncateIndicators(response.getSuggestions()));

                String responseJson = objectMapper.writeValueAsString(response);
                history.setAiAnalysisResponse(responseJson);
            }

            return historyRepository.save(history);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save file analysis history: " + e.getMessage(), e);
        }
    }

    /**
     * Save description analysis response to history (AI Analysis)
     */
    public HistoryReport saveDescriptionAnalysis(String jobDescription, AiAnalysisResponseDto response, UserCred user) {
        try {
            HistoryReport history = new HistoryReport();
            history.setScanType("DESCRIPTION");
            history.setJobDescription(jobDescription);
            history.setUser(user);
            history.setCreatedAt(LocalDateTime.now());
            history.setUpdatedAt(LocalDateTime.now());

            if (response != null) {
                history.setRiskLevel(response.getRiskLevel());
                history.setTrustScore(response.getTrustScore());
                history.setEmail(response.getEmail());
                history.setUrl(response.getUrl());
                history.setCompanyName(response.getCompanyName());
                history.setCompanyDetails(response.getCompanyDetails());
                history.setEmailVerified(response.getEmailVerified());
                history.setCompanyVerified(response.getCompanyVerified());
                history.setAnalysisSummary(response.getAnalysisSummary());
                // Truncate indicators to fit VARCHAR(255) column limit
                history.setSuspiciousIndicators(truncateIndicators(response.getSuspiciousIndicators()));
                history.setSuggestions(truncateIndicators(response.getSuggestions()));

                String responseJson = objectMapper.writeValueAsString(response);
                history.setAiAnalysisResponse(responseJson);
            }

            return historyRepository.save(history);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save description analysis history: " + e.getMessage(), e);
        }
    }

    /**
     * Save URL analysis response to history (with company name extraction)
     */
    public HistoryReport saveUrlAnalysisWithCompany(String url, UrlAnalysisResponse response, String companyName, UserCred user) {
        try {
            HistoryReport history = new HistoryReport();
            history.setScanType("URL");
            history.setUrl(url);
            history.setUser(user);
            history.setCreatedAt(LocalDateTime.now());
            history.setUpdatedAt(LocalDateTime.now());

            if (response != null) {
                history.setRiskLevel(response.getRiskLevel());
                history.setTrustScore(response.getTrustScore());
                history.setCompanyName(companyName);
                history.setAnalysisSummary(response.getAnalysisSummary());
                history.setSuspiciousIndicators(response.getSuspiciousIndicators());
                history.setSuggestions(response.getSuggestions());
                history.setRiskScore(response.getRiskScore());

                String responseJson = objectMapper.writeValueAsString(response);
                history.setUrlAnalysisResponse(responseJson);
            }

            return historyRepository.save(history);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save URL analysis history: " + e.getMessage(), e);
        }
    }

    /**
     * Save email analysis response to history (with company name extraction)
     */
    public HistoryReport saveEmailAnalysisWithCompany(String email, EmailAnalysisResponse response, String companyName, UserCred user) {
        try {
            HistoryReport history = new HistoryReport();
            history.setScanType("EMAIL");
            history.setEmail(email);
            history.setUser(user);
            history.setCreatedAt(LocalDateTime.now());
            history.setUpdatedAt(LocalDateTime.now());

            if (response != null) {
                history.setRiskLevel(response.getRiskLevel());
                history.setTrustScore(response.getTrustScore() != null ? response.getTrustScore().doubleValue() : null);
                history.setCompanyName(companyName);
                history.setRiskScore(response.getRiskScore());
                history.setAnalysisSummary(response.getAnalysisSummary());
                history.setSuspiciousIndicators(response.getSuspiciousIndicators());
                history.setSuggestions(response.getSuggestions());

                String responseJson = objectMapper.writeValueAsString(response);
                history.setEmailAnalysisResponse(responseJson);
            }

            return historyRepository.save(history);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save email analysis history: " + e.getMessage(), e);
        }
    }

    /**
     * Get all scan history for a user
     */
    public List<HistoryReport> getUserHistory(UserCred user) {
        return historyRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Get scan history of a specific type for a user
     */
    public List<HistoryReport> getUserHistoryByType(UserCred user, String scanType) {
        return historyRepository.findByUserAndScanType(user, scanType);
    }

    /**
     * Get HIGH risk scans for a user
     */
    public List<HistoryReport> getHighRiskScans(UserCred user) {
        return historyRepository.findHighRiskScans(user);
    }

    /**
     * Get scan history by risk level
     */
    public List<HistoryReport> getUserHistoryByRiskLevel(UserCred user, String riskLevel) {
        return historyRepository.findByUserAndRiskLevel(user, riskLevel);
    }

    /**
     * Get single scan record by ID
     */
    public HistoryReport getScanById(Long scanId) {
        return historyRepository.findById(scanId).orElse(null);
    }

    /**
     * Delete a scan record
     */
    public void deleteScan(Long scanId) {
        historyRepository.deleteById(scanId);
    }

    /**
     * Delete all scans for a user
     */
    public void deleteUserHistory(UserCred user) {
        historyRepository.deleteByUser(user);
    }

    /**
     * Get total scan count for a user
     */
    public long getTotalScansCount(UserCred user) {
        return historyRepository.countByUser(user);
    }

    /**
     * Get scan count by type for a user
     */
    public long getScanCountByType(UserCred user, String scanType) {
        return historyRepository.countByUserAndScanType(user, scanType);
    }

    /**
     * Get scan history within a date range
     */
    public List<HistoryReport> getScansByDateRange(UserCred user, LocalDateTime startDate, LocalDateTime endDate) {
        return historyRepository.findByUserAndDateRange(user, startDate, endDate);
    }

    /**
     * Deserialize URL analysis response from stored JSON
     */
    public UrlAnalysisResponse getUrlAnalysisResponse(HistoryReport history) {
        if (history.getUrlAnalysisResponse() != null) {
            try {
                return objectMapper.readValue(history.getUrlAnalysisResponse(), UrlAnalysisResponse.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize URL analysis response: " + e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Deserialize email analysis response from stored JSON
     */
    public EmailAnalysisResponse getEmailAnalysisResponse(HistoryReport history) {
        if (history.getEmailAnalysisResponse() != null) {
            try {
                return objectMapper.readValue(history.getEmailAnalysisResponse(), EmailAnalysisResponse.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize email analysis response: " + e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Deserialize AI analysis response from stored JSON
     */
    public AiAnalysisResponseDto getAiAnalysisResponse(HistoryReport history) {
        if (history.getAiAnalysisResponse() != null) {
            try {
                return objectMapper.readValue(history.getAiAnalysisResponse(), AiAnalysisResponseDto.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize AI analysis response: " + e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Truncate indicators/suggestions to fit the VARCHAR(255) database column limit.
     * Each indicator is truncated to 250 characters to be safe.
     * If the list is null, returns null.
     */
    private java.util.List<String> truncateIndicators(java.util.List<String> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            return indicators;
        }
        
        java.util.List<String> truncated = new java.util.ArrayList<>();
        final int MAX_LENGTH = 250; // Leave 5 chars safety margin for 255 limit
        
        for (String indicator : indicators) {
            if (indicator != null) {
                if (indicator.length() > MAX_LENGTH) {
                    String truncatedIndicator = indicator.substring(0, MAX_LENGTH) + "...";
                    truncated.add(truncatedIndicator);
                } else {
                    truncated.add(indicator);
                }
            }
        }
        
        return truncated;
    }
}