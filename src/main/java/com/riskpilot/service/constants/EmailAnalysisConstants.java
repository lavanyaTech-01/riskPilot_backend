package com.riskpilot.service.constants;

import java.util.List;
import java.util.Set;

/**
 * Constants for email analysis including risk scores, free email providers,
 * suspicious keywords, and known company domains.
 */
public class EmailAnalysisConstants {

    // ==================== RISK SCORE CONSTANTS ====================
    public static final int SCORE_FREE_EMAIL_PROVIDER = 20;
    public static final int SCORE_UNKNOWN_DOMAIN = 25;
    public static final int SCORE_LOOKALIKE_DOMAIN = 35;
    public static final int SCORE_SUSPICIOUS_KEYWORD = 15;
    public static final int SCORE_SUSPICIOUS_EMAIL_INDICATOR = 12;
    public static final int SCORE_GENERIC_DOMAIN = 20;

    // Trust score bonus for trusted domains
    public static final int TRUST_BONUS_TRUSTED_DOMAIN = -30;

    // ==================== RISK LEVEL THRESHOLDS ====================
    public static final int RISK_LEVEL_LOW_THRESHOLD = 25;
    public static final int RISK_LEVEL_MEDIUM_THRESHOLD = 50;

    // ==================== TRUST SCORE CONSTANTS ====================
    public static final int TRUST_SCORE_MIN = 0;
    public static final int TRUST_SCORE_MAX = 100;
    public static final int RISK_SCORE_MAX = 100;
    public static final int RISK_SCORE_MIN = 0;

    // ==================== DOMAIN TYPE CLASSIFICATION ====================
    public static final String DOMAIN_TYPE_TRUSTED = "TRUSTED";
    public static final String DOMAIN_TYPE_UNKNOWN = "UNKNOWN";
    public static final String DOMAIN_TYPE_SUSPICIOUS = "SUSPICIOUS";

    // Default trust scores for domain types
    public static final int DEFAULT_TRUST_SCORE_TRUSTED = 85;
    public static final int DEFAULT_TRUST_SCORE_UNKNOWN = 50;
    public static final int DEFAULT_TRUST_SCORE_SUSPICIOUS = 20;

    // ==================== TRUSTED DOMAINS ====================
    // Well-known, verified company domains that are definitely safe
    public static final Set<String> TRUSTED_DOMAINS = Set.of(
            // Tech Giants (Very High Trust)
            "google.com", "microsoft.com", "apple.com", "amazon.com", "meta.com",
            "netflix.com", "tesla.com", "uber.com", "airbnb.com", "linkedin.com",
            "facebook.com", "twitter.com", "instagram.com", "youtube.com", "github.com",
            "slack.com", "discord.com", "zoom.us",
            // IT & Software
            "ibm.com", "oracle.com", "salesforce.com", "adobe.com", "vmware.com",
            "cisco.com", "intel.com", "nvidia.com", "qualcomm.com", "broadcom.com",
            // IT Services & Consulting
            "tcs.com", "infosys.com", "wipro.com", "accenture.com", "deloitte.com",
            "pwc.com", "kpmg.com", "ey.com", "bain.com", "mckinsey.com",
            // Finance
            "jpmorgan.com", "goldman.com", "morgan-stanley.com", "bankofamerica.com",
            "bofa.com", "wellsfargo.com", "citigroup.com", "chase.com",
            // Professional Services
            "capgemini.com", "cognizant.com", "globant.com",
            // Retail & E-commerce
            "walmart.com", "target.com", "costco.com", "bestbuy.com", "ebay.com",
            // Airlines & Travel
            "delta.com", "united.com", "southwest.com", "american.com", "expedia.com", "booking.com",
            // Telecom
            "att.com", "verizon.com", "tmobile.com", "comcast.com"
    );

    // ==================== FREE EMAIL PROVIDERS ====================
    public static final Set<String> FREE_EMAIL_PROVIDERS = Set.of(
            "gmail.com", "yahoo.com", "outlook.com", "hotmail.com",
            "aol.com", "mail.com", "protonmail.com", "tutanota.com",
            "yandex.com", "mail.ru", "qq.com", "163.com",
            "126.com", "sina.com", "sohu.com", "foxmail.com"
    );

    // ==================== SUSPICIOUS EMAIL KEYWORDS ====================
    // Keywords commonly found in email domains used for phishing/scams
    public static final Set<String> SUSPICIOUS_EMAIL_KEYWORDS = Set.of(
            "jobs", "hiring", "recruitment", "careers", "job",
            "verify", "confirm", "urgent", "immediate",
            "apply", "offer", "position", "opportunity",
            "interview", "selected", "qualify", "approve",
            "hr", "human", "resources", "hr-jobs",
            "career-job", "job-offer", "job-recruit"
    );

    // ==================== SUSPICIOUS CONTENT KEYWORDS ====================
    // Keywords commonly found in scam job descriptions and emails
    public static final Set<String> SUSPICIOUS_CONTENT_KEYWORDS = Set.of(
            // Financial/Payment related
            "wire transfer", "bank account", "routing number", "swift",
            "payment", "process payment", "deposit", "investment",
            "bitcoin", "cryptocurrency", "gift card", "money transfer",
            "western union", "moneygram", "paypal", "stripe",
            // Personal information
            "ssn", "social security", "tax id", "passport",
            "driver license", "credit card", "bank details",
            // Urgency indicators
            "urgent", "asap", "immediately", "quickly", "rush",
            "limited time", "act now", "don't delay",
            // Vague promises
            "easy money", "work from home", "no experience",
            "high salary", "guaranteed", "unlimited income",
            "passive income", "quick cash",
            // Verification/confirmation (common phishing tactic)
            "verify account", "confirm identity", "validate", "authenticate",
            "click here", "open link", "update information"
    );

    // ==================== KNOWN COMPANY DOMAINS ====================
    // Common legitimate company domains for lookalike detection
    public static final List<String> KNOWN_COMPANY_DOMAINS = List.of(
            // Tech Giants
            "amazon.com", "google.com", "microsoft.com", "apple.com", "meta.com",
            "netflix.com", "tesla.com", "uber.com", "airbnb.com", "linkedin.com",
            "facebook.com", "twitter.com", "instagram.com", "youtube.com", "github.com",
            // IT & Software
            "ibm.com", "oracle.com", "salesforce.com", "adobe.com", "vmware.com",
            "cisco.com", "intel.com", "nvidia.com", "qualcomm.com", "broadcom.com",
            // IT Services & Consulting
            "tcs.com", "infosys.com", "wipro.com", "accenture.com", "deloitte.com",
            "pwc.com", "kpmg.com", "ey.com", "bain.com", "mckinsey.com",
            // Finance
            "jpmorgan.com", "goldman.com", "morgan-stanley.com", "bankofamerica.com",
            "bofa.com", "wellsfargo.com", "citigroup.com",
            // Retail & Commerce
            "walmart.com", "target.com", "costco.com", "target.com", "bestbuy.com",
            // Airlines & Travel
            "delta.com", "united.com", "southwest.com", "american.com",
            // Telecom
            "att.com", "verizon.com", "tmobile.com"
    );

    // ==================== SIMILARITY THRESHOLD ====================
    public static final int LOOKALIKE_SIMILARITY_THRESHOLD = 75;
}
