package com.riskpilot.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileExtractionService {

    /**
     * Detects the file type and extracts readable text content from the uploaded file.
     */
    public String extractText(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        if (contentType == null && fileName == null) {
            throw new IllegalArgumentException("Unable to determine file type.");
        }

        if (isPdf(contentType, fileName)) {
            return extractFromPdf(file);
        } else if (isImage(contentType, fileName)) {
            return extractFromImage(file);
        } else if (isText(contentType, fileName)) {
            return extractFromText(file);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }
    }

    /**
     * Returns the detected file type category.
     */
    public String detectFileType(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        if (isPdf(contentType, fileName)) return "PDF";
        if (isImage(contentType, fileName)) return "IMAGE";
        if (isText(contentType, fileName)) return "TEXT";
        return "UNKNOWN";
    }

    // ---- File type detection helpers ----

    private boolean isPdf(String contentType, String fileName) {
        return "application/pdf".equalsIgnoreCase(contentType)
                || (fileName != null && fileName.toLowerCase().endsWith(".pdf"));
    }

    private boolean isImage(String contentType, String fileName) {
        if (contentType != null && contentType.startsWith("image/")) return true;
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg") || lower.endsWith(".tiff")
                    || lower.endsWith(".bmp") || lower.endsWith(".gif");
        }
        return false;
    }

    private boolean isText(String contentType, String fileName) {
        if (contentType != null && contentType.startsWith("text/")) return true;
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            return lower.endsWith(".txt") || lower.endsWith(".csv")
                    || lower.endsWith(".log") || lower.endsWith(".md");
        }
        return false;
    }

    // ---- Extraction methods ----

    private String extractFromPdf(MultipartFile file) {
        try {
            PDDocument document = Loader.loadPDF(file.getBytes());
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            document.close();
            return text.trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }

    private String extractFromImage(MultipartFile file) {
        try {
            // Convert image file to Base64 string for Gemini Vision API
            byte[] fileBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(fileBytes);
            
            // Return Base64 encoded image with media type marker
            // Format: "IMAGE_BASE64:{mediaType}:{base64String}"
            String mediaType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            return "IMAGE_BASE64:" + mediaType + ":" + base64Image;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process image file: " + e.getMessage(), e);
        }
    }

    private String extractFromText(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n")).trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read text file: " + e.getMessage(), e);
        }
    }
}
