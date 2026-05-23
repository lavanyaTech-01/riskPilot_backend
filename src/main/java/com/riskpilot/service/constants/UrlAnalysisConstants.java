package com.riskpilot.service.constants;

import java.util.List;
import java.util.Set;

/**
 * Constants for URL analysis including risk scores, trusted domains, and suspicious keywords.
 */
public class UrlAnalysisConstants {

    // ==================== RISK SCORE CONSTANTS ====================
    public static final int SCORE_CROSS_DOMAIN_REDIRECT = 15;
    public static final int SCORE_NEWLY_REGISTERED = 30;
    public static final int SCORE_INVALID_SSL = 15;
    public static final int SCORE_INVALID_DNS = 20;
    public static final int SCORE_MALWARE_FLAGGED = 50;
    public static final int SCORE_TYPOSQUATTING = 25;
    public static final int SCORE_SUSPICIOUS_TLD = 20;
    public static final int SCORE_IP_ADDRESS = 30;
    public static final int SCORE_MANY_PARAMETERS = 10;
    public static final int SCORE_ENCODED_CHARS = 5;
    public static final int SCORE_SUSPICIOUS_KEYWORD = 20;
    public static final int SCORE_MULTIPLE_HYPHENS = 15;
    public static final int SCORE_COMPANY_MIMICRY = 25;

    // Trust score deductions for trusted domains
    public static final int TRUST_BONUS_REDIRECT_TO_TRUSTED = -20;
    public static final int TRUST_BONUS_TRUSTED_DOMAIN = -15;

    // ==================== RISK LEVEL THRESHOLDS ====================
    public static final int RISK_LEVEL_LOW_THRESHOLD = 25;
    public static final int RISK_LEVEL_MEDIUM_THRESHOLD = 50;

    // ==================== TRUST SCORE CONSTANTS ====================
    public static final double TRUST_SCORE_MIN = 0.0;
    public static final double TRUST_SCORE_MAX = 10.0;
    public static final int RISK_SCORE_MAX = 100;
    public static final int RISK_SCORE_MIN = 0;

    // ==================== DOMAIN AGE THRESHOLDS ====================
    public static final int NEW_DOMAIN_THRESHOLD_DAYS = 90;
    public static final int VERY_NEW_DOMAIN_THRESHOLD_DAYS = 7;

    // ==================== API ENDPOINTS ====================
    public static final String VIRUSTOTAL_URL_API = "https://www.virustotal.com/api/v3/urls";
    public static final String VIRUSTOTAL_DOMAIN_API = "https://www.virustotal.com/api/v3/domains";
    public static final String GEMINI_API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent";

    // ==================== TIMEOUT CONSTANTS ====================
    public static final long HTTP_REQUEST_TIMEOUT_SECONDS = 5;
    public static final long GEMINI_REQUEST_TIMEOUT_SECONDS = 10;
    public static final int MAX_REDIRECT_DEPTH = 5;
    public static final int MAX_CONTENT_SIZE_KB = 10;

    // ==================== TRUSTED DOMAINS ====================
    public static final Set<String> TRUSTED_DOMAINS = Set.of(
            // Major tech companies
            "google.com", "microsoft.com", "apple.com", "amazon.com", "meta.com",
            "ibm.com", "oracle.com", "salesforce.com", "adobe.com", "vmware.com",
            "cisco.com", "intel.com", "nvidia.com", "qualcomm.com", "broadcom.com",
            // Collaboration & communication
            "slack.com", "discord.com", "telegram.org", "skype.com", "zoom.us",
            // Social & professional
            "linkedin.com", "facebook.com", "twitter.com", "instagram.com", "youtube.com",
            // Development & hosting
            "github.com", "gitlab.com", "bitbucket.org", "heroku.com", "netlify.com",
            "vercel.app", "render.com", "railway.app", "replit.com",
            // Business & productivity
            "atlassian.com", "jira.com", "confluence.com", "notion.so",
            "asana.com", "trello.com", "miro.com", "figma.com",
            // Documentation & learning
            "wikipedia.org", "stackoverflow.com", "medium.com", "dev.to",
            "hashnode.com", "reddit.com", "quora.com",
            // Email & messaging
            "gmail.com", "outlook.com", "yahoo.com",
            // Payment & financial
            "stripe.com", "paypal.com", "square.com", "shopify.com",
            // IT Services & Consulting
            "tcs.com", "infosys.com", "wipro.com", "accenture.com", "deloitte.com",
            "pwc.com", "kpmg.com", "ey.com", "bain.com", "mckinsey.com"
    );

    // ==================== URL SHORTENERS ====================
    public static final Set<String> URL_SHORTENERS = Set.of(
            "bit.ly", "tinyurl.com", "short.link", "goo.gl", "ow.ly",
            "lnkd.in", "linkedin.shortlink", "t.co", "buff.ly", "adf.ly",
            "shortened.link", "go.pardot.com", "utm.to", "short.news",
            "snip.ly", "rebrand.ly", "tiny.cc", "tr.im", "x.co",
            "short.onl", "cutt.ly", "clck.ru"
    );

    // ==================== KNOWN COMPANY DOMAINS ====================
    public static final List<String> KNOWN_COMPANY_DOMAINS = List.of(
            "amazon.com", "google.com", "microsoft.com", "apple.com", "meta.com",
            "netflix.com", "tesla.com", "uber.com", "airbnb.com", "linkedin.com",
            "facebook.com", "twitter.com", "instagram.com", "youtube.com", "github.com",
            "ibm.com", "oracle.com", "salesforce.com", "adobe.com", "vmware.com",
            "cisco.com", "intel.com", "nvidia.com", "qualcomm.com", "broadcom.com",
            "tcs.com", "infosys.com", "accenture.com", "deloitte.com", "pwc.com",
            // Google services subdomains
            "docs.google.com", "forms.google.com", "sheets.google.com", "slides.google.com",
            "drive.google.com", "mail.google.com", "calendar.google.com",
            // Microsoft services subdomains
            "outlook.live.com", "onedrive.live.com", "teams.microsoft.com",
            // Other common trusted services
            "github.io", "heroku.com", "netlify.com", "vercel.app"
    );

    // ==================== SUSPICIOUS KEYWORDS ====================
    public static final List<String> SUSPICIOUS_KEYWORDS = List.of(
            "registration-fee", "registration fee", "verify", "confirm", "update", "validate",
            "account", "urgent", "action required", "click here", "activate", "suspended",
            "limited time", "confirm-identity", "secure", "sign-in", "login", "password",
            "payment", "billing", "credit", "refund", "reward", "claim", "winner",
            "security", "alert", "unusual", "suspicious", "unauthorized", "immediate"
    );

    // ==================== SUSPICIOUS TLDs ====================
    public static final String SUSPICIOUS_TLDS_REGEX = ".*\\.(xyz|tk|ml|ga|cf|gq|top|buzz|click|loan|racing)(/.*)?$";

    // ==================== PRIVATE CONSTRUCTOR ====================
    private UrlAnalysisConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}
