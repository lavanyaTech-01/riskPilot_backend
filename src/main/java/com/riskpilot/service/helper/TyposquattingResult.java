package com.riskpilot.service.helper;

/**
 * Result object for typosquatting detection operations.
 * Contains information about whether typosquatting was detected and similarity score.
 */
public class TyposquattingResult {
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

    @Override
    public String toString() {
        return "TyposquattingResult{" +
                "detected=" + detected +
                ", similarityScore=" + similarityScore +
                '}';
    }
}
