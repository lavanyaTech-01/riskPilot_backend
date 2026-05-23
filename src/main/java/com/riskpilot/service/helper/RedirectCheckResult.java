package com.riskpilot.service.helper;

/**
 * Result object for redirect detection operations.
 * Contains information about whether a redirect was detected and the final URL.
 */
public class RedirectCheckResult {
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

    @Override
    public String toString() {
        return "RedirectCheckResult{" +
                "originalUrl='" + originalUrl + '\'' +
                ", finalUrl='" + finalUrl + '\'' +
                ", redirectDetected=" + redirectDetected +
                ", redirectMethod='" + redirectMethod + '\'' +
                '}';
    }
}
