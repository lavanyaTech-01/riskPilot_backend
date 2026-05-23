package com.riskpilot.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.riskpilot.model.UrlAnalysisRequest;
import com.riskpilot.model.UrlAnalysisResponse;
import com.riskpilot.service.constants.SecurityConfiguration;
import com.riskpilot.service.constants.UrlAnalysisConstants;
import com.riskpilot.service.helper.MalwareCheckResult;
import com.riskpilot.service.helper.RedirectCheckResult;
import com.riskpilot.service.helper.TyposquattingResult;

/**
 * Service that analyzes URLs for scam indicators and security risks.
 * Performs multiple verification checks including:
 * - Domain age analysis
 * - SSL certificate validation
 * - Redirect detection
 * - Malware/blacklist checking (VirusTotal API)
 * - Typosquatting detection
 * - DNS resolution
 */
@Service
public class UrlAnalysisService {

    @Autowired
    private AiAnalysisService aiAnalysisService;

    private static final Logger logger = LoggerFactory.getLogger(UrlAnalysisService.class);

    @Value("${virustotal.api.key:}")
    private String virusTotalApiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final HttpClient httpClientNoRedirect = HttpClient.newBuilder()
            .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Main method to analyze a URL for scam and security indicators.
     * Performs comprehensive checks and returns a risk assessment.
     * PRIORITY: Analyzes final redirect URL, not original URL.
     */
    public UrlAnalysisResponse analyzeUrl(UrlAnalysisRequest request) {
        UrlAnalysisResponse response = new UrlAnalysisResponse();
        
        // Initialize all fields to ensure they are never null
        response.setRiskLevel("UNKNOWN");
        response.setTrustScore(0.0);
        response.setRiskScore(0);
        response.setSslValid(false);
        response.setMalwareFlagged(false);
        response.setMalwareEngineCount(0);
        response.setDnsResolvable(false);
        response.setRedirectDetected(false);
        response.setTyposquattingDetected(false);
        response.setTyposquattingSimilarityScore(0);
        response.setSuspiciousIndicators(new ArrayList<>());
        response.setSuggestions(new ArrayList<>());
        response.setFlaggedSecurityEngines(new ArrayList<>());
        
        List<String> indicators = new ArrayList<>();
        int riskScore = 0;

        try {
            // Validate and normalize the URL
            String normalizedUrl = normalizeUrl(request.getUrl());
            String originalDomain = extractDomain(normalizedUrl);
            logger.info("🔍 URL Analysis Started - URL: {}, Domain: {}", normalizedUrl, originalDomain);
            
            // PRIORITY: Get final redirect URL early
            RedirectCheckResult redirectResult = null;
            try {
                redirectResult = detectRedirects(normalizedUrl);
                logger.info("✅ Redirect detection completed - Redirected: {}", 
                    (redirectResult != null && redirectResult.isRedirectDetected()) ? "YES" : "NO");
            } catch (Exception e) {
                logger.error("❌ Redirect detection failed: {}", e.getMessage());
                redirectResult = new RedirectCheckResult();
                redirectResult.setFinalUrl(normalizedUrl);
            }
            
            String analysisUrl = normalizedUrl;
            String analysisDomain = originalDomain;
            boolean isShortener = isUrlShortener(originalDomain);
            
            if (redirectResult != null && redirectResult.getFinalUrl() != null) {
                response.setFinalRedirectUrl(redirectResult.getFinalUrl());
                response.setRedirectDetected(redirectResult.isRedirectDetected());
                logger.info("📍 Final Redirect URL: {}", redirectResult.getFinalUrl());
                
                // PRIORITY: Use final URL for analysis
                if (redirectResult.isRedirectDetected()) {
                    analysisUrl = redirectResult.getFinalUrl();
                    analysisDomain = extractDomain(analysisUrl);
                    
                    response.setTargetCompany(analysisDomain);
                    response.setTargetAnalysisDetails(redirectResult.getRedirectMethod() + ": " + redirectResult.getRedirectInfo());
                    logger.info("🎯 Target Company: {}, Redirect Method: {}", analysisDomain, redirectResult.getRedirectMethod());
                    
                    // Apply context-aware redirect logic
                    if (isShortener) {
                        logger.debug("🔗 Redirect from URL shortener {} - normal behavior", originalDomain);
                    } else if (isTrustedDomain(analysisDomain)) {
                        logger.debug("✅ Redirect to trusted domain {} - safe", analysisDomain);
                        riskScore += UrlAnalysisConstants.TRUST_BONUS_REDIRECT_TO_TRUSTED;
                    } else {
                        logger.debug("⚠️ Unknown domain redirect - minor risk");
                        riskScore += UrlAnalysisConstants.SCORE_CROSS_DOMAIN_REDIRECT;
                    }
                } else {
                    response.setFinalRedirectUrl(normalizedUrl);
                }
            } else {
                response.setFinalRedirectUrl(normalizedUrl);
                response.setRedirectDetected(false);
                logger.info("ℹ️ No redirect detected, analyzing original URL");
            }
            
            response.setDomain(originalDomain);

            // 1. Domain Age Check (on analysis domain)
            int domainAgeDays = -1;
            try {
                domainAgeDays = checkDomainAge(analysisDomain);
                response.setDomainAgeDays(domainAgeDays);
                logger.info("📅 Domain Age: {} days", domainAgeDays);
                if (domainAgeDays >= 0 && domainAgeDays < UrlAnalysisConstants.NEW_DOMAIN_THRESHOLD_DAYS) {
                    indicators.add("Newly registered domain (age: " + domainAgeDays + " days)");
                    int penalty = isTrustedDomain(analysisDomain) ? 10 : UrlAnalysisConstants.SCORE_NEWLY_REGISTERED;
                    riskScore += penalty;
                    logger.warn("⚠️ Domain is very new - age: {} days, risk penalty: {}", domainAgeDays, penalty);
                }
            } catch (Exception e) {
                logger.error("❌ Domain age check failed: {}", e.getMessage());
                response.setDomainAgeDays(-1);
            }

            // 2. SSL Certificate Verification
            try {
                boolean sslValid = verifySSLCertificate(analysisUrl);
                response.setSslValid(sslValid);
                logger.info("🔐 SSL Valid: {}", sslValid);
                if (!sslValid && analysisUrl.startsWith("https")) {
                    indicators.add("Invalid or expired SSL certificate");
                    riskScore += UrlAnalysisConstants.SCORE_INVALID_SSL;
                    logger.warn("⚠️ SSL certificate is invalid or expired");
                }
            } catch (Exception e) {
                logger.error("❌ SSL verification failed: {}", e.getMessage());
                response.setSslValid(false);
                indicators.add("SSL verification failed: " + e.getMessage());
                riskScore += UrlAnalysisConstants.SCORE_INVALID_SSL;
            }

            // 4. DNS Resolution
            try {
                boolean dnsResolvable = resolveDns(analysisDomain);
                response.setDnsResolvable(dnsResolvable);
                logger.info("🌐 DNS Resolvable: {}", dnsResolvable);
                if (!dnsResolvable) {
                    indicators.add("Domain does not resolve (invalid DNS record)");
                    riskScore += UrlAnalysisConstants.SCORE_INVALID_DNS;
                    logger.warn("⚠️ Domain does not have valid DNS records");
                }
            } catch (Exception e) {
                logger.error("❌ DNS resolution failed: {}", e.getMessage());
                response.setDnsResolvable(false);
                indicators.add("DNS resolution failed: " + e.getMessage());
                riskScore += UrlAnalysisConstants.SCORE_INVALID_DNS;
            }

            // 5. Malware / Blacklist Check (if VirusTotal API key is available)
            try {
                if (virusTotalApiKey != null && !virusTotalApiKey.isEmpty()) {
                    MalwareCheckResult malwareResult = checkMalwareWithVirusTotal(analysisDomain);
                    response.setMalwareFlagged(malwareResult.isFlagged());
                    response.setMalwareEngineCount(malwareResult.getFlaggedEngineCount());
                    response.setFlaggedSecurityEngines(malwareResult.getFlaggedEngines());
                    logger.info("🦠 Malware Flagged: {}, Engine Count: {}", malwareResult.isFlagged(), malwareResult.getFlaggedEngineCount());
                    if (malwareResult.isFlagged()) {
                        indicators.add("Domain flagged by " + malwareResult.getFlaggedEngineCount() + " security engines");
                        riskScore += UrlAnalysisConstants.SCORE_MALWARE_FLAGGED;
                        logger.warn("⚠️ Malware detected by {} security engines", malwareResult.getFlaggedEngineCount());
                    }
                } else {
                    logger.info("ℹ️ VirusTotal API key not configured, skipping malware check");
                    response.setMalwareFlagged(false);
                    response.setMalwareEngineCount(0);
                }
            } catch (Exception e) {
                logger.error("❌ Malware check failed: {}", e.getMessage());
                response.setMalwareFlagged(false);
                response.setMalwareEngineCount(0);
            }

            // 6. Typosquatting Detection (on analysis domain)
            try {
                TyposquattingResult typosquatResult = detectTyposquatting(analysisDomain, request.getKnownCompanyDomain());
                response.setTyposquattingDetected(typosquatResult.isDetected());
                response.setTyposquattingSimilarityScore(typosquatResult.getSimilarityScore());
                logger.info("🎭 Typosquatting Detected: {}, Score: {}", typosquatResult.isDetected(), typosquatResult.getSimilarityScore());
                if (typosquatResult.isDetected()) {
                    indicators.add("Potential typosquatting detected (similarity: " + typosquatResult.getSimilarityScore() + "%)");
                    riskScore += UrlAnalysisConstants.SCORE_TYPOSQUATTING;
                    logger.warn("⚠️ Possible typosquatting domain - similarity score: {}%", typosquatResult.getSimilarityScore());
                }
            } catch (Exception e) {
                logger.error("❌ Typosquatting detection failed: {}", e.getMessage());
                response.setTyposquattingDetected(false);
                response.setTyposquattingSimilarityScore(0);
            }

            // Trusted domain bonus - significantly reduce risk
            if (isTrustedDomain(analysisDomain)) {
                riskScore += UrlAnalysisConstants.TRUST_BONUS_TRUSTED_DOMAIN;
                indicators.add("URL redirects to/uses trusted domain: " + analysisDomain);
                logger.info("✅ Domain is trusted - applying trust bonus");
            }

            // Initialize risk score in response before URL pattern analysis
            response.setRiskScore(Math.max(0, riskScore));
            logger.info("📊 Risk Score before URL pattern analysis: {}", response.getRiskScore());

            // 8. URL Pattern Analysis (on analysis domain)
            try {
                analyzeUrlPattern(analysisUrl, indicators, response);
                logger.info("✅ URL pattern analysis completed");
            } catch (Exception e) {
                logger.error("❌ URL pattern analysis failed: {}", e.getMessage());
            }

            // Recalculate risk score and trust score after pattern analysis
            riskScore = response.getRiskScore();
            response.setTrustScore(convertRiskScoreToTrustScore(riskScore));
            response.setRiskLevel(getRiskLevel(riskScore));
            response.setSuspiciousIndicators(indicators);
            
            logger.info("📊 Final Risk Score: {}, Trust Score: {}, Risk Level: {}", 
                response.getRiskScore(), response.getTrustScore(), response.getRiskLevel());

            // Generate suggestions
            try {
                List<String> suggestions = generateSuggestions(response);
                response.setSuggestions(suggestions);
                logger.info("💡 Generated {} suggestions", suggestions.size());
            } catch (Exception e) {
                logger.error("❌ Suggestion generation failed: {}", e.getMessage());
                response.setSuggestions(new ArrayList<>());
            }

            // Generate summary
            try {
                String summary = generateAnalysisSummary(response);
                response.setAnalysisSummary(summary);
                logger.info("📝 Analysis summary generated");
            } catch (Exception e) {
                logger.error("❌ Summary generation failed: {}", e.getMessage());
                response.setAnalysisSummary("Analysis completed but summary generation failed.");
            }

            logger.info("✅ URL Analysis Completed Successfully - Risk Level: {}", response.getRiskLevel());
            return response;

        } catch (Exception e) {
            logger.error("❌ URL Analysis Failed: {}", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to analyze URL: " + e.getMessage(), e);
        }
    }

    /**
     * Normalizes the URL by adding protocol if missing and handling encoding.
     */
    private String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        return url;
    }

    /**
     * Extracts the domain name from a URL.
     * Example: https://www.example.com/path -> example.com
     */
    private String extractDomain(String urlString) {
        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("Invalid URL: " + urlString);
            }
            // Remove www prefix if present
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract domain: " + e.getMessage());
        }
    }

    /**
     * Checks the domain age by attempting WHOIS lookup.
     * Returns the age in days, or -1 if unable to determine.
     */
    private int checkDomainAge(String domain) {
        try {
            // For now, we'll implement a simple pattern-based check
            // In a production environment, you would use a WHOIS library or API
            // This is a placeholder that returns a default value
            // You can integrate with APIs like whoisxmlapi.com or domaintools.com

            // Simulated check - in production, implement real WHOIS lookup
            LocalDate creationDate = performWhoisLookup(domain);
            if (creationDate != null) {
                LocalDate today = LocalDate.now();
                return (int) java.time.temporal.ChronoUnit.DAYS.between(creationDate, today);
            }

            return -1; // Unknown
        } catch (Exception e) {
            return -1; // Unknown due to error
        }
    }

    /**
     * Performs a WHOIS lookup to get domain creation date.
     * Uses multiple strategies: WhoisXML API with retries, DNS analysis, and pattern-based estimation.
     */
    private LocalDate performWhoisLookup(String domain) {
        try {
            // Strategy 1: Try WhoisXML API with retries (most reliable)
            LocalDate result = tryWhoisXmlApiWithRetry(domain, 2);
            if (result != null) {
                return result;
            }
            
            // Strategy 2: Try DNS-based age estimation
            result = estimateAgeViaDns(domain);
            if (result != null) {
                return result;
            }
            
            // Strategy 3: Try alternative fast WHOIS service (with timeout)
            result = tryFastWhoisService(domain);
            if (result != null) {
                return result;
            }
            
            System.err.println("All domain age lookup strategies failed for: " + domain);
            return null;
        } catch (Exception e) {
            System.err.println("WHOIS lookup error for domain: " + domain + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Try WhoisXML API with retry logic
     */
    private LocalDate tryWhoisXmlApiWithRetry(String domain, int maxRetries) {
        int attempt = 0;
        long waitTime = 1000; // Start with 1 second
        
        while (attempt < maxRetries) {
            try {
                attempt++;
                String apiUrl = "https://www.whoisxmlapi.com/api/v1?apiKey=at_n8P2LR85GD5YwMDnVhvUkLyqMOJDe&domain=" + domain + "&outputFormat=JSON";
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(apiUrl))
                        .timeout(Duration.ofSeconds(5)) // Increased timeout
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode responseBody = objectMapper.readTree(response.body());
                    JsonNode whoisRecord = responseBody.path("WhoisRecord");
                    
                    if (!whoisRecord.isMissingNode() && !whoisRecord.isEmpty()) {
                        String createdDateStr = whoisRecord.path("createdDate").asText(null);
                        if (createdDateStr != null && !createdDateStr.isEmpty()) {
                            try {
                                if (createdDateStr.contains("T")) {
                                    return java.time.LocalDateTime.parse(createdDateStr.replace("Z", "")).toLocalDate();
                                } else {
                                    return LocalDate.parse(createdDateStr);
                                }
                            } catch (Exception e) {
                                System.err.println("Failed to parse WhoisXML date: " + createdDateStr);
                            }
                        }
                    }
                } else if (response.statusCode() == 429) {
                    // Rate limited - wait and retry
                    if (attempt < maxRetries) {
                        System.err.println("WhoisXML rate limited, retrying in " + (waitTime/1000) + "s...");
                        Thread.sleep(waitTime);
                        waitTime *= 2; // Exponential backoff
                    }
                }
                
                return null; // Success but no data
            } catch (java.net.http.HttpTimeoutException e) {
                if (attempt < maxRetries) {
                    System.err.println("WhoisXML timeout on attempt " + attempt + ", retrying...");
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    System.err.println("WhoisXML all retries exhausted");
                }
            } catch (Exception e) {
                System.err.println("WhoisXML API error on attempt " + attempt + ": " + e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Estimate domain age using DNS TTL and response headers
     * This is a fast fallback that doesn't require external APIs
     */
    private LocalDate estimateAgeViaDns(String domain) {
        try {
            // Get DNS records which may contain creation date info
            java.net.InetAddress[] addresses = java.net.InetAddress.getAllByName(domain);
            
            if (addresses.length > 0) {
                // If DNS resolves, the domain is active
                // We can estimate it's at least a few days old (minimum 3 days)
                // This is a safe estimate for active domains
                LocalDate today = LocalDate.now();
                LocalDate estimatedCreation = today.minusDays(365); // Conservative: assume 1+ year old
                
                // Check DNS records for more accurate info
                try {
                    javax.naming.directory.InitialDirContext dirContext = new javax.naming.directory.InitialDirContext();
                    javax.naming.directory.Attributes attrs = dirContext.getAttributes("dns://" + domain);
                    
                    // Try to extract SOA record which may have creation info
                    Object soa = attrs.get("SOA");
                    if (soa != null) {
                        // SOA record exists - domain is registered
                        return estimatedCreation;
                    }
                } catch (Exception e) {
                    // Ignore DNS record errors
                }
                
                return estimatedCreation;
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("DNS age estimation failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Try fast WHOIS service as last resort
     * Uses a simpler, faster API with shorter timeout
     */
    private LocalDate tryFastWhoisService(String domain) {
        try {
            // Using a different endpoint that's typically faster
            String apiUrl = "https://whois.whoisxmlapi.com/api/v1/whois?apiKey=at_n8P2LR85GD5YwMDnVhvUkLyqMOJDe&domain=" + domain + "&outputFormat=json&da=new";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(apiUrl))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode responseBody = objectMapper.readTree(response.body());
                
                // Look for createdDate in different locations
                String createdDateStr = responseBody.path("createdDate").asText(null);
                if (createdDateStr == null) {
                    createdDateStr = responseBody.path("WhoisRecord").path("createdDate").asText(null);
                }
                
                if (createdDateStr != null && !createdDateStr.isEmpty()) {
                    try {
                        if (createdDateStr.contains("T")) {
                            return java.time.LocalDateTime.parse(createdDateStr.replace("Z", "")).toLocalDate();
                        } else {
                            return LocalDate.parse(createdDateStr);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to parse fast WHOIS date: " + createdDateStr);
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Fast WHOIS service failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verifies SSL certificate validity for HTTPS URLs.
     */
    private boolean verifySSLCertificate(String urlString) {
        try {
            if (!urlString.startsWith("https://")) {
                return true; // HTTP URLs don't have SSL
            }

            URL url = new URL(urlString);
            url.openConnection().connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Resolves domain to check if it has valid DNS records.
     */
    private boolean resolveDns(String domain) {
        try {
            java.net.InetAddress.getByName(domain);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks domain against VirusTotal API for malware flags.
     */
    private MalwareCheckResult checkMalwareWithVirusTotal(String domain) {
        MalwareCheckResult result = new MalwareCheckResult();

        if (virusTotalApiKey == null || virusTotalApiKey.isEmpty()) {
            return result; // No API key available
        }

        try {
            // Create a URL-encoded query
            String url = UrlAnalysisConstants.VIRUSTOTAL_DOMAIN_API + "/" + domain;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("x-apikey", virusTotalApiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode responseBody = objectMapper.readTree(response.body());

                // Extract last_analysis_stats
                JsonNode lastAnalysisStats = responseBody
                        .path("data")
                        .path("attributes")
                        .path("last_analysis_stats");

                int maliciousCount = lastAnalysisStats.path("malicious").asInt(0);
                int suspiciousCount = lastAnalysisStats.path("suspicious").asInt(0);

                if (maliciousCount > 0 || suspiciousCount > 0) {
                    result.setFlagged(true);
                    result.setFlaggedEngineCount(maliciousCount + suspiciousCount);

                    // Extract flagged engines
                    JsonNode analysisResults = responseBody
                            .path("data")
                            .path("attributes")
                            .path("last_analysis_results");

                    List<String> flaggedEngines = new ArrayList<>();
                    analysisResults.fields().forEachRemaining(entry -> {
                        JsonNode engineResult = entry.getValue();
                        String category = engineResult.path("category").asText("");
                        if ("malicious".equals(category) || "suspicious".equals(category)) {
                            flaggedEngines.add(entry.getKey());
                        }
                    });

                    result.setFlaggedEngines(flaggedEngines);
                }
            }

            return result;
        } catch (Exception e) {
            return result; // Return empty result on error
        }
    }

    /**
     * Detects typosquatting by comparing domain with known legitimate domains.
     * Uses Levenshtein distance algorithm for similarity matching.
     */
    private TyposquattingResult detectTyposquatting(String domain, String knownCompanyDomain) {
        TyposquattingResult result = new TyposquattingResult();

        String domainToCheck = domain.replaceAll("\\d+", "").toLowerCase();
        String cleanDomain = domainToCheck.split("\\.")[0]; // Get just the domain name without TLD

        // If a known company domain is provided, check similarity
        if (knownCompanyDomain != null && !knownCompanyDomain.isEmpty()) {
            String cleanCompany = knownCompanyDomain.toLowerCase().split("\\.")[0];
            int similarity = calculateLevenshteinSimilarity(cleanDomain, cleanCompany);
            result.setSimilarityScore(similarity);
            if (similarity > 60) { // Lowered from 70
                result.setDetected(true);
            }
            return result;
        }

        // Check against list of known domains
        for (String knownDomain : SecurityConfiguration.getKnownCompanyDomains()) {
            String companyName = knownDomain.split("\\.")[0];
            
            // Exact or high similarity match
            int similarity = calculateLevenshteinSimilarity(cleanDomain, companyName);
            if (similarity > 65) { // Lowered from 75
                result.setDetected(true);
                result.setSimilarityScore(similarity);
                return result;
            }
            
            // Check for partial match with hyphens (e.g., "tcs-" from "tcs-registration-fee-careers")
            if (cleanDomain.startsWith(companyName + "-") || 
                cleanDomain.endsWith("-" + companyName) ||
                cleanDomain.contains("-" + companyName + "-")) {
                result.setDetected(true);
                result.setSimilarityScore(80); // High suspicion for company name with hyphens
                return result;
            }
        }

        return result;
    }

    /**
     * Calculates similarity between two strings using Levenshtein distance.
     * Returns a percentage (0-100) where 100 is identical.
     */
    private int calculateLevenshteinSimilarity(String str1, String str2) {
        int distance = levenshteinDistance(str1, str2);
        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0) {
            return 100;
        }
        return Math.round(100 * (maxLength - distance) / (float) maxLength);
    }

    /**
     * Calculates the Levenshtein distance between two strings.
     */
    private int levenshteinDistance(String str1, String str2) {
        int[][] distance = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            distance[i][0] = i;
        }
        for (int j = 0; j <= str2.length(); j++) {
            distance[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                int cost = str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1;
                distance[i][j] = Math.min(
                        Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1),
                        distance[i - 1][j - 1] + cost
                );
            }
        }

        return distance[str1.length()][str2.length()];
    }

    /**
     * Analyzes URL pattern for suspicious characteristics.
     */
    private void analyzeUrlPattern(String url, List<String> indicators, UrlAnalysisResponse response) {
        int additionalRiskScore = 0;
        String domain = extractDomain(url).toLowerCase();
        
        // Check for suspicious TLDs
        if (url.matches(SecurityConfiguration.getSuspiciousTldsPattern())) {
            indicators.add("URL uses suspicious TLD (high-risk domain extension)");
            additionalRiskScore += UrlAnalysisConstants.SCORE_SUSPICIOUS_TLD;
        }

        // Check for IP-based URLs
        if (url.matches("https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}.*")) {
            indicators.add("URL uses IP address instead of domain name");
            additionalRiskScore += UrlAnalysisConstants.SCORE_IP_ADDRESS;
        }

        // Check for lengthy URLs with many parameters (common in phishing)
        if (url.length() > 100) {
            int paramCount = url.split("\\?|&").length - 1;
            if (paramCount > 5) {
                indicators.add("URL contains many suspicious parameters");
                additionalRiskScore += UrlAnalysisConstants.SCORE_MANY_PARAMETERS;
            }
        }

        // Check for special characters that might indicate encoding
        if (url.contains("%") && !url.contains("://")) {
            indicators.add("URL contains encoded characters");
            additionalRiskScore += UrlAnalysisConstants.SCORE_ENCODED_CHARS;
        }
        
        // Check for suspicious keywords - but skip if trusted domain
        if (!isTrustedDomain(domain)) {
            for (String keyword : SecurityConfiguration.getSuspiciousKeywords()) {
                if (domain.contains(keyword.replace(" ", "-")) || domain.contains(keyword.replace(" ", ""))) {
                    indicators.add("Domain contains suspicious keyword: '" + keyword + "'");
                    additionalRiskScore += UrlAnalysisConstants.SCORE_SUSPICIOUS_KEYWORD;
                    break; // Only add once for keyword detection
                }
            }
        }
        
        // Check for multiple hyphens (common phishing pattern) - but skip if trusted domain
        if (!isTrustedDomain(domain)) {
            long hyphenCount = domain.chars().filter(ch -> ch == '-').count();
            if (hyphenCount >= 2) {
                indicators.add("Domain contains multiple hyphens (common phishing pattern)");
                additionalRiskScore += UrlAnalysisConstants.SCORE_MULTIPLE_HYPHENS;
            }
        }
        
        // Check for company name mimicry patterns (like "tcs-" or "-tcs-") - but skip if trusted domain
        if (!isTrustedDomain(domain)) {
            for (String company : SecurityConfiguration.getKnownCompanyDomains()) {
                String companyName = company.split("\\.")[0];
                if (!domain.contains(company) && 
                    (domain.startsWith(companyName + "-") || domain.contains("-" + companyName + "-") || domain.endsWith("-" + companyName))) {
                    indicators.add("Domain appears to mimic company name: " + companyName);
                    additionalRiskScore += UrlAnalysisConstants.SCORE_COMPANY_MIMICRY;
                    break; // Only add once for company mimicry
                }
            }
        }
        
        // Apply additional risk score to response - safely handle null values
        Integer currentScore = response.getRiskScore();
        int currentRiskScore = (currentScore != null) ? currentScore : 0;
        response.setRiskScore(Math.max(0, currentRiskScore + additionalRiskScore));
    }

    /**
     * Converts risk score (0-100) to trust score (0-10).
     * Ensures consistency: high risk = low trust, low risk = high trust
     */
    private double convertRiskScoreToTrustScore(int riskScore) {
        // Clamp risk score to 0-100 range
        int clampedScore = Math.max(0, Math.min(UrlAnalysisConstants.RISK_SCORE_MAX, riskScore));
        
        // Risk score 0 -> Trust score 10
        // Risk score 50 -> Trust score 5
        // Risk score 100 -> Trust score 0
        double trustScore = UrlAnalysisConstants.TRUST_SCORE_MAX - (clampedScore / 10.0);
        return Math.max(UrlAnalysisConstants.TRUST_SCORE_MIN, Math.min(UrlAnalysisConstants.TRUST_SCORE_MAX, trustScore));
    }

    /**
     * Determines risk level based on risk score.
     * Thresholds adjusted to reduce false positives on legitimate URLs.
     */
    private String getRiskLevel(int riskScore) {
        // Clamp risk score to 0-100 range
        int clampedScore = Math.max(0, Math.min(UrlAnalysisConstants.RISK_SCORE_MAX, riskScore));
        
        if (clampedScore <= UrlAnalysisConstants.RISK_LEVEL_LOW_THRESHOLD) {
            return "LOW";
        } else if (clampedScore <= UrlAnalysisConstants.RISK_LEVEL_MEDIUM_THRESHOLD) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }

    /**
     * Generates user-friendly suggestions based on analysis results.
     * Context-aware: provides specific advice for different risk scenarios.
     */
    private List<String> generateSuggestions(UrlAnalysisResponse response) {
        List<String> suggestions = new ArrayList<>();
        
        // If low risk and trusted domain, give minimal warning
        if ("LOW".equals(response.getRiskLevel())) {
            suggestions.add("This URL appears to be safe");
            if (response.getRedirectDetected() != null && response.getRedirectDetected()) {
                suggestions.add("Redirect detected to trusted domain - normal behavior");
            }
            return suggestions;
        }
        
        // If redirects to trusted domain, provide reassurance
        if (response.getRedirectDetected() != null && response.getRedirectDetected() && 
            response.getTargetCompany() != null && isTrustedDomain(response.getTargetCompany())) {
            suggestions.add("Redirects to known trusted domain: " + response.getTargetCompany());
        }
        
        // Specific recommendations for various risk factors
        if (response.getDomainAgeDays() != null && response.getDomainAgeDays() < 7) {
            suggestions.add("Very new domain - verify company through official website before proceeding");
        } else if (response.getDomainAgeDays() != null && response.getDomainAgeDays() < 90) {
            suggestions.add("Recently registered domain - use caution, verify company legitimacy");
        }

        if (response.getSslValid() != null && !response.getSslValid()) {
            suggestions.add("Invalid SSL certificate - do not enter personal or financial information");
        }

        if (response.getMalwareFlagged() != null && response.getMalwareFlagged()) {
            suggestions.add("Domain flagged by security engines - do not visit");
        }

        if (response.getTyposquattingDetected() != null && response.getTyposquattingDetected()) {
            suggestions.add("Domain appears to mimic legitimate company - verify URL carefully");
        }

        if (response.getDnsResolvable() != null && !response.getDnsResolvable()) {
            suggestions.add("Domain does not resolve - may be inactive or fake");
        }

        // General advice based on risk level
        if ("HIGH".equals(response.getRiskLevel())) {
            suggestions.add("High risk detected - strongly recommend NOT visiting this URL");
            suggestions.add("Report this URL if it appears to be a phishing attempt");
        } else if ("MEDIUM".equals(response.getRiskLevel())) {
            suggestions.add("Proceed with caution - verify the source and company legitimacy");
            suggestions.add("Do not enter sensitive credentials unless you're certain it's legitimate");
        }

        return suggestions;
    }

    /**
     * Generates a detailed summary of the analysis.
     */
    private String generateAnalysisSummary(UrlAnalysisResponse response) {
        StringBuilder summary = new StringBuilder();

        summary.append("URL Analysis Summary\n");
        summary.append("====================\n");
        summary.append("Domain: ").append(response.getDomain()).append("\n");
        
        if (response.getFinalRedirectUrl() != null && response.getRedirectDetected() != null && response.getRedirectDetected()) {
            summary.append("Final Destination: ").append(response.getFinalRedirectUrl()).append("\n");
            summary.append("Target Domain: ").append(response.getTargetCompany()).append("\n");
        }
        
        summary.append("\nRisk Assessment\n");
        summary.append("Risk Level: ").append(response.getRiskLevel()).append("\n");
        summary.append("Trust Score: ").append(String.format("%.1f", response.getTrustScore())).append("/10\n");
        summary.append("Risk Score: ").append(response.getRiskScore()).append("/100\n");

        if (response.getSuspiciousIndicators() != null && !response.getSuspiciousIndicators().isEmpty()) {
            summary.append("\nDetected Issues:\n");
            for (String indicator : response.getSuspiciousIndicators()) {
                summary.append("- ").append(indicator).append("\n");
            }
        } else {
            summary.append("\nNo major issues detected.\n");
        }

        return summary.toString();
    }
    /**
     * Comprehensive redirect detection using multiple strategies.
     * Returns a result object containing the final URL and redirect information.
     */
    private RedirectCheckResult detectRedirects(String originalUrl) {
        RedirectCheckResult result = new RedirectCheckResult();
        result.setOriginalUrl(originalUrl);
        result.setFinalUrl(originalUrl); // Default to original if no redirect

        try {
            // Strategy 1: Follow HTTP redirect chain
            RedirectCheckResult httpResult = followHttpRedirectChain(originalUrl, 0, 5);
            if (httpResult != null && httpResult.isFinalUrlDifferent()) {
                result.setFinalUrl(httpResult.getFinalUrl());
                result.setRedirectDetected(true);
                result.setRedirectMethod("HTTP Redirect");
                result.setRedirectInfo(httpResult.getRedirectInfo());
                logger.info("HTTP redirect detected: {} -> {}", originalUrl, httpResult.getFinalUrl());
                return result; // HTTP redirects take priority
            }

            // Strategy 2: Check for meta refresh
            RedirectCheckResult metaResult = detectMetaRefreshRedirect(originalUrl);
            if (metaResult != null && metaResult.isFinalUrlDifferent()) {
                result.setFinalUrl(metaResult.getFinalUrl());
                result.setRedirectDetected(true);
                result.setRedirectMethod("Meta Refresh");
                result.setRedirectInfo(metaResult.getRedirectInfo());
                logger.info("Meta refresh redirect detected: {} -> {}", originalUrl, metaResult.getFinalUrl());
                return result;
            }

            // Strategy 3: Check for JavaScript redirect
            RedirectCheckResult jsResult = detectJavaScriptRedirect(originalUrl);
            if (jsResult != null && jsResult.isFinalUrlDifferent()) {
                result.setFinalUrl(jsResult.getFinalUrl());
                result.setRedirectDetected(true);
                result.setRedirectMethod("JavaScript Redirect");
                result.setRedirectInfo(jsResult.getRedirectInfo());
                logger.info("JavaScript redirect detected: {} -> {}", originalUrl, jsResult.getFinalUrl());
                return result;
            }

            // Strategy 4: Check for frame/iframe redirect
            RedirectCheckResult frameResult = detectFrameRedirect(originalUrl);
            if (frameResult != null && frameResult.isFinalUrlDifferent()) {
                result.setFinalUrl(frameResult.getFinalUrl());
                result.setRedirectDetected(true);
                result.setRedirectMethod("Frame Redirect");
                result.setRedirectInfo(frameResult.getRedirectInfo());
                logger.info("Frame redirect detected: {} -> {}", originalUrl, frameResult.getFinalUrl());
                return result;
            }

            // Strategy 5: AI-Assisted Redirect Detection (using Gemini)
            // This uses AI to analyze page content and extract any referenced URLs
            RedirectCheckResult aiResult = detectRedirectsWithAi(originalUrl);
            if (aiResult != null && aiResult.isFinalUrlDifferent()) {
                result.setFinalUrl(aiResult.getFinalUrl());
                result.setRedirectDetected(true);
                result.setRedirectMethod("AI Detection");
                result.setRedirectInfo(aiResult.getRedirectInfo());
                logger.info("AI-detected redirect: {} -> {}", originalUrl, aiResult.getFinalUrl());
                return result;
            }

            // Strategy 6: Fallback - Capture actual response URL
            RedirectCheckResult urlCaptureResult = captureActualResponseUrl(originalUrl);
            if (urlCaptureResult != null && urlCaptureResult.getFinalUrl() != null) {
                result.setFinalUrl(urlCaptureResult.getFinalUrl());
                if (urlCaptureResult.isFinalUrlDifferent()) {
                    result.setRedirectDetected(true);
                    result.setRedirectMethod("Automatic");
                    logger.debug("Automatic redirect detected: {} -> {}", originalUrl, urlCaptureResult.getFinalUrl());
                } else {
                    logger.debug("No explicit redirect, but final URL captured: {}", urlCaptureResult.getFinalUrl());
                }
            }

            return result;
        } catch (Exception e) {
            logger.error("Error during redirect detection: {}", e.getMessage());
            return result; // Return result with original URL
        }
    }

    /**
     * Follows HTTP redirect chain (301, 302, 303, 307, 308).
     * Recursively follows redirects up to maxDepth to prevent infinite loops.
     */
    private RedirectCheckResult followHttpRedirectChain(String urlString, int depth, int maxDepth) {
        RedirectCheckResult result = new RedirectCheckResult();
        result.setOriginalUrl(urlString);
        result.setFinalUrl(urlString);
        List<String> redirectChain = new ArrayList<>();
        redirectChain.add(urlString);

        if (depth >= maxDepth) {
            logger.debug("Max redirect depth ({}) reached. Final URL: {}", maxDepth, urlString);
            result.setFinalUrl(urlString);
            return result;
        }

        String currentUrl = urlString;
        int currentDepth = depth;
        
        try {
            while (currentDepth < maxDepth) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(currentUrl))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClientNoRedirect.send(request, HttpResponse.BodyHandlers.ofString());
                
                int statusCode = response.statusCode();
                logger.debug("URL: {} - Status Code: {}", currentUrl, statusCode);

                // Check if it's a redirect response (3xx status code)
                if (statusCode >= 300 && statusCode < 400) {
                    // Get the Location header which points to the next URL
                    java.util.Optional<String> location = response.headers().firstValue("Location");
                    
                    if (location.isPresent()) {
                        String nextUrl = location.get();
                        
                        // Resolve relative URLs
                        if (!nextUrl.startsWith("http")) {
                            try {
                                nextUrl = new URL(new URL(currentUrl), nextUrl).toString();
                            } catch (Exception e) {
                                logger.debug("Could not resolve relative URL: {}", nextUrl);
                                break;
                            }
                        }

                        logger.info("Redirect detected: {} -> {} (Status: {})", currentUrl, nextUrl, statusCode);
                        redirectChain.add(nextUrl);
                        currentUrl = nextUrl;
                        currentDepth++;
                    } else {
                        logger.debug("Redirect response but no Location header found");
                        break;
                    }
                } else {
                    // Not a redirect, we've reached the final destination
                    logger.debug("Final destination reached: {}", currentUrl);
                    break;
                }
            }

            // Set the final URL after following all redirects
            result.setFinalUrl(currentUrl);
            
            if (!currentUrl.equals(urlString)) {
                result.setRedirectDetected(true);
                
                // Build redirect chain info
                StringBuilder chainInfo = new StringBuilder();
                for (int i = 0; i < redirectChain.size(); i++) {
                    if (i > 0) chainInfo.append(" -> ");
                    chainInfo.append(redirectChain.get(i));
                }
                
                // Check if it's a cross-domain redirect (phishing indicator)
                String originalDomain = extractDomain(urlString);
                String finalDomain = extractDomain(currentUrl);

                if (!originalDomain.equals(finalDomain)) {
                    result.setRedirectInfo("Cross-domain redirect chain: " + originalDomain + " to " + finalDomain + " (PHISHING RISK)");
                } else {
                    result.setRedirectInfo("Internal redirect chain within " + originalDomain + " (" + redirectChain.size() + " hops)");
                }
                
                logger.info("Complete redirect chain: {}", chainInfo.toString());
            }

            return result;
        } catch (Exception e) {
            logger.debug("HTTP redirect chain follow failed for {}: {}", urlString, e.getMessage());
            result.setFinalUrl(currentUrl);
            return result;
        }
    }

    /**
     * Detects meta refresh redirects by fetching the HTML and parsing for meta tags.
     * Recursively follows the chain to the final destination.
     */
    private RedirectCheckResult detectMetaRefreshRedirect(String urlString) {
        return detectMetaRefreshRedirectChain(urlString, 0, 5, new ArrayList<>());
    }

    /**
     * Recursively detects meta refresh redirects following the complete chain.
     */
    private RedirectCheckResult detectMetaRefreshRedirectChain(String urlString, int depth, int maxDepth, List<String> visitedUrls) {
        RedirectCheckResult result = new RedirectCheckResult();
        result.setOriginalUrl(urlString);
        result.setFinalUrl(urlString);

        if (depth >= maxDepth) {
            logger.debug("Max meta refresh depth ({}) reached at: {}", maxDepth, urlString);
            return result;
        }

        // Prevent infinite loops
        if (visitedUrls.contains(urlString)) {
            logger.debug("Circular meta refresh detected at: {}", urlString);
            result.setFinalUrl(urlString);
            return result;
        }
        visitedUrls.add(urlString);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(urlString))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String htmlContent = response.body();

            // Pattern to match: <meta http-equiv="refresh" content="delay; url=...">
            String metaPattern = "(?i)<meta\\s+http-equiv\\s*=\\s*['\"]?refresh['\"]?\\s+content\\s*=\\s*['\"]?[^'\"]*url\\s*=\\s*([^'\"\\s>]+)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(metaPattern);
            java.util.regex.Matcher matcher = pattern.matcher(htmlContent);

            if (matcher.find()) {
                String redirectUrl = matcher.group(1).replaceAll("['\"]", "").trim();
                
                // Resolve relative URLs
                if (!redirectUrl.startsWith("http")) {
                    try {
                        redirectUrl = new URL(new URL(urlString), redirectUrl).toString();
                    } catch (Exception e) {
                        logger.debug("Could not resolve relative meta refresh URL: {}", redirectUrl);
                    }
                }

                logger.info("Meta refresh redirect detected: {} -> {}", urlString, redirectUrl);

                // Recursively follow the chain
                RedirectCheckResult nextResult = detectMetaRefreshRedirectChain(redirectUrl, depth + 1, maxDepth, new ArrayList<>(visitedUrls));
                
                result.setFinalUrl(nextResult.getFinalUrl());
                result.setRedirectDetected(true);
                result.setRedirectInfo("Meta refresh chain to: " + nextResult.getFinalUrl());
                
                return result;
            }

            result.setFinalUrl(urlString);
            return result;
        } catch (Exception e) {
            logger.debug("Meta refresh detection failed for {}: {}", urlString, e.getMessage());
            result.setFinalUrl(urlString);
            return result;
        }
    }

    /**
     * Detects JavaScript-based redirects by parsing HTML for window.location and related patterns.
     * Recursively follows the chain to the final destination.
     */
    private RedirectCheckResult detectJavaScriptRedirect(String urlString) {
        return detectJavaScriptRedirectChain(urlString, 0, 5, new ArrayList<>());
    }

    /**
     * Recursively detects JavaScript redirects following the complete chain.
     */
    private RedirectCheckResult detectJavaScriptRedirectChain(String urlString, int depth, int maxDepth, List<String> visitedUrls) {
        RedirectCheckResult result = new RedirectCheckResult();
        result.setOriginalUrl(urlString);
        result.setFinalUrl(urlString);

        if (depth >= maxDepth) {
            logger.debug("Max JS redirect depth ({}) reached at: {}", maxDepth, urlString);
            return result;
        }

        // Prevent infinite loops
        if (visitedUrls.contains(urlString)) {
            logger.debug("Circular JS redirect detected at: {}", urlString);
            result.setFinalUrl(urlString);
            return result;
        }
        visitedUrls.add(urlString);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(urlString))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String htmlContent = response.body();

            // Patterns for various JS redirect methods
            String[] jsPatterns = {
                "(?i)window\\.location\\s*=\\s*['\"]([^'\"]+)['\"]",
                "(?i)window\\.location\\.href\\s*=\\s*['\"]([^'\"]+)['\"]",
                "(?i)location\\.replace\\s*\\(['\"]([^'\"]+)['\"]",
                "(?i)location\\.href\\s*=\\s*['\"]([^'\"]+)['\"]"
            };

            for (String jsPattern : jsPatterns) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(jsPattern);
                java.util.regex.Matcher matcher = pattern.matcher(htmlContent);

                if (matcher.find()) {
                    String redirectUrl = matcher.group(1).trim();

                    // Resolve relative URLs
                    if (!redirectUrl.startsWith("http")) {
                        try {
                            redirectUrl = new URL(new URL(urlString), redirectUrl).toString();
                        } catch (Exception e) {
                            logger.debug("Could not resolve relative JS redirect URL: {}", redirectUrl);
                        }
                    }

                    logger.info("JavaScript redirect detected: {} -> {}", urlString, redirectUrl);

                    // Recursively follow the chain
                    RedirectCheckResult nextResult = detectJavaScriptRedirectChain(redirectUrl, depth + 1, maxDepth, new ArrayList<>(visitedUrls));
                    
                    result.setFinalUrl(nextResult.getFinalUrl());
                    result.setRedirectDetected(true);
                    result.setRedirectInfo("JavaScript redirect chain to: " + nextResult.getFinalUrl());
                    
                    return result;
                }
            }

            result.setFinalUrl(urlString);
            return result;
        } catch (Exception e) {
            logger.debug("JavaScript redirect detection failed for {}: {}", urlString, e.getMessage());
            result.setFinalUrl(urlString);
            return result;
        }
    }

    /**
     * Detects frame/iframe redirects by parsing HTML for frame/iframe src attributes.
     * Recursively follows the chain to the final destination.
     */
    private RedirectCheckResult detectFrameRedirect(String urlString) {
        return detectFrameRedirectChain(urlString, 0, 5, new ArrayList<>());
    }

    /**
     * Recursively detects frame/iframe redirects following the complete chain.
     */
    private RedirectCheckResult detectFrameRedirectChain(String urlString, int depth, int maxDepth, List<String> visitedUrls) {
        RedirectCheckResult result = new RedirectCheckResult();
        result.setOriginalUrl(urlString);
        result.setFinalUrl(urlString);

        if (depth >= maxDepth) {
            logger.debug("Max frame redirect depth ({}) reached at: {}", maxDepth, urlString);
            return result;
        }

        // Prevent infinite loops
        if (visitedUrls.contains(urlString)) {
            logger.debug("Circular frame redirect detected at: {}", urlString);
            result.setFinalUrl(urlString);
            return result;
        }
        visitedUrls.add(urlString);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(urlString))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String htmlContent = response.body();

            // Pattern for iframe/frame src attributes
            String framePattern = "(?i)<(?:iframe|frame)\\s+[^>]*src\\s*=\\s*['\"]?([^'\"\\s>]+)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(framePattern);
            java.util.regex.Matcher matcher = pattern.matcher(htmlContent);

            if (matcher.find()) {
                String redirectUrl = matcher.group(1).replaceAll("['\"]", "").trim();

                // Resolve relative URLs
                if (!redirectUrl.startsWith("http")) {
                    try {
                        redirectUrl = new URL(new URL(urlString), redirectUrl).toString();
                    } catch (Exception e) {
                        logger.debug("Could not resolve relative frame URL: {}", redirectUrl);
                    }
                }

                logger.info("Frame redirect detected: {} -> {}", urlString, redirectUrl);

                // Recursively follow the chain
                RedirectCheckResult nextResult = detectFrameRedirectChain(redirectUrl, depth + 1, maxDepth, new ArrayList<>(visitedUrls));
                
                result.setFinalUrl(nextResult.getFinalUrl());
                result.setRedirectDetected(true);
                result.setRedirectInfo("Frame redirect chain to: " + nextResult.getFinalUrl());
                
                return result;
            }

            result.setFinalUrl(urlString);
            return result;
        } catch (Exception e) {
            logger.debug("Frame redirect detection failed for {}: {}", urlString, e.getMessage());
            result.setFinalUrl(urlString);
            return result;
        }
    }

    /**
     * Fallback method: Captures the actual response URL after following the complete redirect chain.
     * This method ALWAYS succeeds and ensures finalRedirectUrl is never NULL.
     * Uses the same chain-following logic as HTTP redirects.
     */
    private RedirectCheckResult captureActualResponseUrl(String urlString) {
        RedirectCheckResult result = new RedirectCheckResult();
        result.setOriginalUrl(urlString);

        String currentUrl = urlString;
        int depth = 0;
        int maxDepth = 5;
        List<String> urlChain = new ArrayList<>();
        urlChain.add(urlString);

        try {
            while (depth < maxDepth) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(currentUrl))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClientNoRedirect.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();

                // Check if it's a redirect response
                if (statusCode >= 300 && statusCode < 400) {
                    java.util.Optional<String> location = response.headers().firstValue("Location");
                    
                    if (location.isPresent()) {
                        String nextUrl = location.get();
                        
                        // Resolve relative URLs
                        if (!nextUrl.startsWith("http")) {
                            try {
                                nextUrl = new URL(new URL(currentUrl), nextUrl).toString();
                            } catch (Exception e) {
                                break;
                            }
                        }

                        urlChain.add(nextUrl);
                        currentUrl = nextUrl;
                        depth++;
                    } else {
                        break;
                    }
                } else {
                    // Not a redirect, final destination reached
                    break;
                }
            }

            result.setFinalUrl(currentUrl);
            return result;
        } catch (Exception e) {
            logger.debug("Actual URL capture failed for {}: {}", urlString, e.getMessage());
            // Even on error, return what we have
            result.setFinalUrl(currentUrl);
            return result;
        }
    }

    /**
     * Normalizes URL for comparison by removing www, query params, and fragments.
     */
    private String normalizeUrlForComparison(String url) {
        try {
            URL u = new URL(url);
            String normalized = u.getProtocol() + "://" + u.getHost() + u.getPath();
            if (normalized.startsWith("https://www.")) {
                normalized = normalized.replace("https://www.", "https://");
            } else if (normalized.startsWith("http://www.")) {
                normalized = normalized.replace("http://www.", "http://");
            }
            return normalized;
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * Helper class to store malware check results.
     */
    private static class MalwareCheckResult {
        private boolean flagged;
        private int flaggedEngineCount;
        private List<String> flaggedEngines;

        public boolean isFlagged() {
            return flagged;
        }

        public void setFlagged(boolean flagged) {
            this.flagged = flagged;
        }

        public int getFlaggedEngineCount() {
            return flaggedEngineCount;
        }

        public void setFlaggedEngineCount(int flaggedEngineCount) {
            this.flaggedEngineCount = flaggedEngineCount;
        }

        public List<String> getFlaggedEngines() {
            return flaggedEngines;
        }

        public void setFlaggedEngines(List<String> flaggedEngines) {
            this.flaggedEngines = flaggedEngines;
        }
    }

    /**
     * Helper class to store typosquatting detection results.
     */
    private static class TyposquattingResult {
        private boolean detected;
        private int similarityScore;

        public boolean isDetected() {
            return detected;
        }

        public void setDetected(boolean detected) {
            this.detected = detected;
        }

        public int getSimilarityScore() {
            return similarityScore;
        }

        public void setSimilarityScore(int similarityScore) {
            this.similarityScore = similarityScore;
        }
    }

    /**
     * Helper class to store redirect check results.
     */
    private static class RedirectCheckResult {
        private String originalUrl;
        private String finalUrl;
        private boolean redirectDetected;
        private String redirectMethod;
        private String redirectInfo;

        public String getOriginalUrl() {
            return originalUrl;
        }

        public void setOriginalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
        }

        public String getFinalUrl() {
            return finalUrl;
        }

        public void setFinalUrl(String finalUrl) {
            this.finalUrl = finalUrl;
        }

        public boolean isRedirectDetected() {
            return redirectDetected;
        }

        public void setRedirectDetected(boolean redirectDetected) {
            this.redirectDetected = redirectDetected;
        }

        public String getRedirectMethod() {
            return redirectMethod;
        }

        public void setRedirectMethod(String redirectMethod) {
            this.redirectMethod = redirectMethod;
        }

        public String getRedirectInfo() {
            return redirectInfo;
        }

        public void setRedirectInfo(String redirectInfo) {
            this.redirectInfo = redirectInfo;
        }

        /**
         * Checks if the final URL is different from the original URL.
         */
        public boolean isFinalUrlDifferent() {
            return finalUrl != null && !finalUrl.equals(originalUrl);
        }
    }

    /**
     * AI-Assisted Redirect Detection using Gemini API.
     * Fetches the URL content and uses AI to analyze and extract any embedded URLs
     * that might indicate redirects or the final destination.
     */
    private RedirectCheckResult detectRedirectsWithAi(String urlString) {
        RedirectCheckResult result = new RedirectCheckResult();
        result.setOriginalUrl(urlString);
        result.setFinalUrl(urlString);

        if (aiAnalysisService == null) {
            logger.debug("AI Analysis Service not available for redirect detection");
            return result;
        }

        try {
            // Fetch the page content
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(urlString))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String pageContent = response.body();

            if (pageContent == null || pageContent.isEmpty()) {
                logger.debug("Empty page content from: {}", urlString);
                return result;
            }

            // Truncate very large content to avoid API limits
            if (pageContent.length() > 10000) {
                pageContent = pageContent.substring(0, 10000) + "\n... (content truncated)";
            }

            // Build AI prompt to extract embedded URLs
            String aiPrompt = buildRedirectDetectionPrompt(urlString, pageContent);

            // Use simple HTTP call to Gemini API for URL extraction
            String extractedUrls = callGeminiForUrlExtraction(aiPrompt);

            if (extractedUrls != null && !extractedUrls.isEmpty()) {
                // Parse the AI response to extract final URL
                String finalUrl = parseExtractedUrl(extractedUrls);
                
                if (finalUrl != null && !finalUrl.equals(urlString)) {
                    result.setFinalUrl(finalUrl);
                    result.setRedirectDetected(true);
                    result.setRedirectInfo("AI detected embedded URL: " + finalUrl);
                    logger.info("AI redirect detection successful: {} -> {}", urlString, finalUrl);
                    return result;
                }
            }

            return result;
        } catch (Exception e) {
            logger.debug("AI-assisted redirect detection failed for {}: {}", urlString, e.getMessage());
            return result;
        }
    }

    /**
     * Builds a prompt for AI to extract embedded URLs from page content.
     */
    private String buildRedirectDetectionPrompt(String originalUrl, String pageContent) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Analyze the following web page content from URL: ").append(originalUrl).append("\n\n");
        prompt.append("Your task: Find and extract any embedded URLs that might indicate:\n");
        prompt.append("1. Redirect destinations (meta refresh, JavaScript, forms)\n");
        prompt.append("2. Links to external pages\n");
        prompt.append("3. Suspicious URLs (phishing indicators)\n");
        prompt.append("4. The actual destination URL the page is trying to send users to\n\n");
        
        prompt.append("Return ONLY a JSON object with this structure (no markdown, no code blocks):\n");
        prompt.append("{\n");
        prompt.append("  \"finalRedirectUrl\": \"<most likely final destination URL or null>\",\n");
        prompt.append("  \"embeddedUrls\": [\"<url1>\", \"<url2>\", ...],\n");
        prompt.append("  \"redirectType\": \"<http|meta|javascript|form|none>\",\n");
        prompt.append("  \"suspiciousIndicators\": [\"<indicator1>\", \"<indicator2>\", ...]\n");
        prompt.append("}\n\n");
        
        prompt.append("Page Content:\n");
        prompt.append(pageContent);
        
        return prompt.toString();
    }

    /**
     * Calls Gemini API directly to extract URLs from page content.
     */
    private String callGeminiForUrlExtraction(String prompt) {
        try {
            String geminiApiKey = System.getenv("GEMINI_API_KEY");
            if (geminiApiKey == null) {
                // Try to get from Spring config - fall back to placeholder
                logger.debug("GEMINI_API_KEY not found in environment");
                return null;
            }

            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=" + geminiApiKey;

            // Build request body
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode requestBody = mapper.createObjectNode();
            ArrayNode contentsArray = mapper.createArrayNode();
            ObjectNode content = mapper.createObjectNode();
            ArrayNode partsArray = mapper.createArrayNode();
            ObjectNode part = mapper.createObjectNode();
            part.put("text", prompt);
            partsArray.add(part);
            content.set("parts", partsArray);
            contentsArray.add(content);
            requestBody.set("contents", contentsArray);

            String requestBodyStr = mapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyStr))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode responseJson = mapper.readTree(response.body());
                JsonNode candidates = responseJson.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode content1 = candidates.get(0).path("content");
                    JsonNode parts = content1.path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        String text = parts.get(0).path("text").asText();
                        return text;
                    }
                }
            } else {
                logger.debug("Gemini API error status: {}", response.statusCode());
            }

            return null;
        } catch (Exception e) {
            logger.debug("Error calling Gemini API for URL extraction: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parses the AI response to extract the final redirect URL.
     */
    private String parseExtractedUrl(String aiResponse) {
        try {
            // Try to extract JSON from AI response (it might have extra text)
            int jsonStart = aiResponse.indexOf("{");
            int jsonEnd = aiResponse.lastIndexOf("}");
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = aiResponse.substring(jsonStart, jsonEnd + 1);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(jsonStr);
                
                // Get the final redirect URL from AI response
                String finalUrl = json.path("finalRedirectUrl").asText(null);
                
                if (finalUrl != null && !finalUrl.isEmpty() && !finalUrl.equals("null")) {
                    // Validate it's a proper URL
                    if (finalUrl.startsWith("http://") || finalUrl.startsWith("https://")) {
                        return finalUrl;
                    }
                }
                
                // If no finalRedirectUrl, try to get from embeddedUrls
                JsonNode embeddedUrls = json.path("embeddedUrls");
                if (embeddedUrls.isArray() && embeddedUrls.size() > 0) {
                    String firstUrl = embeddedUrls.get(0).asText();
                    if (firstUrl != null && !firstUrl.isEmpty()) {
                        return firstUrl;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.debug("Error parsing AI response for URL extraction: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if a domain is in the trusted domains list.
     * Trusted domains significantly reduce risk assessment penalties.
     */
    private boolean isTrustedDomain(String domain) {
        if (domain == null || domain.isEmpty()) {
            return false;
        }
        
        // Normalize domain
        String normalizedDomain = domain.toLowerCase();
        if (normalizedDomain.startsWith("www.")) {
            normalizedDomain = normalizedDomain.substring(4);
        }
        
        // Check exact match using security config
        Set<String> trustedDomains = SecurityConfiguration.getTrustedDomains();
        if (trustedDomains.contains(normalizedDomain)) {
            return true;
        }
        
        // Check for subdomains of trusted domains (e.g., api.github.com -> github.com)
        String[] parts = normalizedDomain.split("\\.");
        if (parts.length > 2) {
            String parentDomain = String.join(".", parts[parts.length - 2], parts[parts.length - 1]);
            if (trustedDomains.contains(parentDomain)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Checks if a domain is a known URL shortener.
     * Shortener redirects are normal, not a phishing indicator.
     */
    private boolean isUrlShortener(String domain) {
        if (domain == null || domain.isEmpty()) {
            return false;
        }
        
        // Normalize domain
        String normalizedDomain = domain.toLowerCase();
        if (normalizedDomain.startsWith("www.")) {
            normalizedDomain = normalizedDomain.substring(4);
        }
        
        // Check exact match using security config
        Set<String> shorteners = SecurityConfiguration.getUrlShorteners();
        if (shorteners.contains(normalizedDomain)) {
            return true;
        }
        
        // Check for subdomains of URL shorteners
        String[] parts = normalizedDomain.split("\\.");
        if (parts.length > 2) {
            String parentDomain = String.join(".", parts[parts.length - 2], parts[parts.length - 1]);
            if (shorteners.contains(parentDomain)) {
                return true;
            }
        }
        
        return false;
    }
}
