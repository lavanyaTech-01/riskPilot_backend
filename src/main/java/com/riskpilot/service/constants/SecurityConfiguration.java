package com.riskpilot.service.constants;

import java.util.Set;

/**
 * Security configuration for URL analysis.
 * Contains trusted domains, URL shorteners, and related configurations.
 */
public class SecurityConfiguration {

    /**
     * List of trusted domains that are considered safe.
     * Redirects to these domains are not flagged as phishing.
     */
    public static Set<String> getTrustedDomains() {
        return Set.of(
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
    }

    /**
     * List of common URL shortener domains.
     * Redirects from these domains are expected and not flagged as phishing.
     */
    public static Set<String> getUrlShorteners() {
        return Set.of(
                "bit.ly", "tinyurl.com", "short.link", "goo.gl", "ow.ly",
                "lnkd.in", "linkedin.shortlink", "t.co", "buff.ly", "adf.ly",
                "shortened.link", "go.pardot.com", "utm.to", "short.news",
                "snip.ly", "rebrand.ly", "tiny.cc", "tr.im", "x.co",
                "short.onl", "cutt.ly", "clck.ru"
        );
    }

    /**
     * List of known company domains used for typosquatting detection.
     */
    public static Set<String> getKnownCompanyDomains() {
        return Set.of(
                "amazon.com", "google.com", "microsoft.com", "apple.com", "meta.com",
                "netflix.com", "tesla.com", "uber.com", "airbnb.com", "linkedin.com",
                "facebook.com", "twitter.com", "instagram.com", "youtube.com", "github.com",
                "ibm.com", "oracle.com", "salesforce.com", "adobe.com", "vmware.com",
                "cisco.com", "intel.com", "nvidia.com", "qualcomm.com", "broadcom.com",
                "tcs.com", "infosys.com", "accenture.com", "deloitte.com", "pwc.com",
                // Google services
                "docs.google.com", "forms.google.com", "sheets.google.com", "slides.google.com",
                "drive.google.com", "mail.google.com", "calendar.google.com",
                // Microsoft services
                "outlook.live.com", "onedrive.live.com", "teams.microsoft.com",
                // Other trusted services
                "github.io", "heroku.com", "netlify.com", "vercel.app"
        );
    }

    /**
     * List of suspicious keywords commonly found in phishing URLs.
     */
    public static Set<String> getSuspiciousKeywords() {
        return Set.of(
                "registration-fee", "registration fee", "verify", "confirm", "update", "validate",
                "account", "urgent", "action required", "click here", "activate", "suspended",
                "limited time", "confirm-identity", "secure", "sign-in", "login", "password",
                "payment", "billing", "credit", "refund", "reward", "claim", "winner",
                "security", "alert", "unusual", "suspicious", "unauthorized", "immediate"
        );
    }

    /**
     * Regex pattern for detecting suspicious TLDs.
     */
    public static String getSuspiciousTldsPattern() {
        return ".*\\.(xyz|tk|ml|ga|cf|gq|top|buzz|click|loan|racing)(/.*)?$";
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private SecurityConfiguration() {
        throw new AssertionError("Cannot instantiate SecurityConfiguration");
    }
}
