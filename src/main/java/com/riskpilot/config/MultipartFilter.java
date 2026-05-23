package com.riskpilot.config;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter to ensure multipart requests are handled properly.
 * Runs FIRST in the filter chain (Order.HIGHEST_PRECEDENCE) to ensure
 * multipart requests bypass other filters that might consume the body.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MultipartFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String contentType = request.getContentType();
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        // Log multipart requests for debugging
        if (contentType != null && contentType.contains("multipart/form-data")) {
            System.out.println("========================================");
            System.out.println("✓ MULTIPART REQUEST DETECTED");
            System.out.println("  Method: " + method);
            System.out.println("  URI: " + requestURI);
            System.out.println("  Content-Type: " + contentType);
            System.out.println("  Content-Length: " + request.getContentLength() + " bytes");
            System.out.println("========================================");
        }
        
        // Pass request through filter chain
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String contentType = request.getContentType();
        // Skip this filter entirely for multipart requests - don't consume body
        return contentType != null && contentType.contains("multipart/form-data");
    }
}
