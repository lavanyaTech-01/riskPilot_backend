package com.riskpilot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * Parses raw extracted text to identify structured fields such as
 * email addresses, URLs, and company names for AI analysis.
 * 
 * (AI-Only implementation - No rule-based logic)
 */
@Service
public class ContentParserService {

    // Regex to match email addresses
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    // Regex to match URLs (http, https, ftp, and www)
    private static final Pattern URL_PATTERN =
            Pattern.compile("(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+|www\\.[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)");

    // Common keywords that often precede a company name in job postings
    private static final Pattern COMPANY_PATTERN = Pattern.compile(
            "(?i)(?:company\\s*(?:name)?\\s*[:;\\-]?\\s*|" +
            "organization\\s*[:;\\-]?\\s*|" +
            "employer\\s*[:;\\-]?\\s*|" +
            "firm\\s*[:;\\-]?\\s*|" +
            "hiring\\s+for\\s*[:;\\-]?\\s*|" +
            "at\\s+)([A-Z][A-Za-z0-9&.,'\\-\\s]{2,50})");

    // Well-known suffixes that indicate a company name
    private static final Pattern COMPANY_SUFFIX_PATTERN = Pattern.compile(
            "(?i)\\b([A-Z][A-Za-z0-9&.,'\\-\\s]{1,50}\\s+(?:Pvt\\.?\\s*Ltd\\.?|" +
            "Private\\s+Limited|Ltd\\.?|LLC|Inc\\.?|Corp\\.?|Corporation|" +
            "Technologies|Solutions|Consulting|Services|Group|Enterprises))\\b");

    /**
     * Extracts all email addresses found in the text.
     */
    public List<String> extractAllEmails(String text) {
        return extractAll(EMAIL_PATTERN, text);
    }

    /**
     * Extracts all URLs found in the text.
     */
    public List<String> extractAllUrls(String text) {
        return extractAll(URL_PATTERN, text);
    }

    /**
     * Extracts company name from text using multiple strategies.
     */
    public String extractCompanyName(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        // Strategy 1: Look for "Join <Company>" pattern
        Pattern joinPattern = Pattern.compile("(?i)join\\s+([A-Za-z0-9\\s&.,'\\-]+?)(?:\\s+(?:as|at|for|Inc|Ltd|LLC|Pvt|Corp|Co\\.|Company|hiring|job|position|for))");
        Matcher joinMatcher = joinPattern.matcher(text);
        if (joinMatcher.find()) {
            String company = joinMatcher.group(1).trim();
            if (company.length() > 2 && !isCommonWord(company)) {
                System.out.println("✓ Company detected via 'Join' pattern: " + company);
                return company;
            }
        }
        
        // Strategy 2: Look for "at <Company>" pattern
        Pattern atPattern = Pattern.compile("(?i)at\\s+([A-Za-z0-9\\s&.,'\\-]+?)(?:\\s+(?:Inc|Ltd|LLC|Pvt|Corp|hiring|job|position|career|careers|apply))");
        Matcher atMatcher = atPattern.matcher(text);
        if (atMatcher.find()) {
            String company = atMatcher.group(1).trim();
            if (company.length() > 2 && !isCommonWord(company)) {
                System.out.println("✓ Company detected via 'at' pattern: " + company);
                return company;
            }
        }
        
        // Strategy 3: Look for company with suffix (Inc, Ltd, LLC, Pvt Ltd, etc.)
        Pattern suffixPattern = Pattern.compile("\\b([A-Za-z0-9\\s&.,'\\-]+?)\\s+(?:Inc\\.?|Ltd\\.?|LLC|Pvt\\.?|Corp\\.?|Co\\.?|Limited|Private\\s+Limited|Corporation|Technologies|Solutions|Consulting|Services|Group|Enterprises)\\b");
        Matcher suffixMatcher = suffixPattern.matcher(text);
        if (suffixMatcher.find()) {
            String company = suffixMatcher.group(1).trim();
            if (company.length() > 2) {
                System.out.println("✓ Company detected via suffix pattern: " + company);
                return company;
            }
        }
        
        // Strategy 4: Look for "<Company> hiring" pattern
        Pattern hiringPattern = Pattern.compile("([A-Za-z0-9\\s&.,'\\-]+?)\\s+(?:is\\s+)?(?:hiring|recruiting|looking)");
        Matcher hiringMatcher = hiringPattern.matcher(text);
        if (hiringMatcher.find()) {
            String company = hiringMatcher.group(1).trim();
            if (company.length() > 2 && !isCommonWord(company)) {
                System.out.println("✓ Company detected via 'hiring' pattern: " + company);
                return company;
            }
        }
        
        // Strategy 5: Look for "Company:" or "Organization:" keywords
        Pattern keywordPattern = Pattern.compile("(?i)(?:company|organization|employer|firm)\\s*[:=]\\s*([A-Za-z0-9\\s&.,'\\-]+?)(?:\\n|\\r|$|,|;)");
        Matcher keywordMatcher = keywordPattern.matcher(text);
        if (keywordMatcher.find()) {
            String company = keywordMatcher.group(1).trim();
            if (company.length() > 2) {
                System.out.println("✓ Company detected via keyword pattern: " + company);
                return company;
            }
        }
        
        // Strategy 6: Extract capitalized phrases (last resort)
        Pattern capitalPattern = Pattern.compile("\\b([A-Z][A-Za-z0-9]*(?:\\s+[A-Z][A-Za-z0-9]*)?)\\b");
        Matcher capitalMatcher = capitalPattern.matcher(text);
        
        if (capitalMatcher.find()) {
            String candidate = capitalMatcher.group(1).trim();
            if (candidate.length() > 2 && !isCommonWord(candidate) && !isStopWord(candidate)) {
                System.out.println("✓ Company detected via capitalization: " + candidate);
                return candidate;
            }
        }

        System.out.println("⚠️ No company name detected");
        return null;
    }
    
    /**
     * Checks if a word is a common English word (not a company name)
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
     * Checks if a word is a common stop word
     */
    private boolean isStopWord(String word) {
        String[] stopWords = {"A", "An", "The", "And", "Or", "But", "In", "On", "At", "To", "For",
                             "Of", "From", "Up", "About", "Out", "If", "As", "By", "Is", "Are"};
        for (String stop : stopWords) {
            if (word.equalsIgnoreCase(stop)) {
                return true;
            }
        }
        return false;
    }

    // ---- Private helpers ----

    private String extractFirst(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group().trim() : null;
    }

    private List<String> extractAll(Pattern pattern, String text) {
        List<String> results = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String match = matcher.group().trim();
            if (!results.contains(match)) {
                results.add(match);
            }
        }
        return results;
    }
}
