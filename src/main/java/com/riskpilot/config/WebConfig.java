package com.riskpilot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for handling multipart file uploads.
 * Configures the StandardServletMultipartResolver to properly handle multipart requests
 * for file upload endpoints like /api/scan/file.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	/**
	 * Configures the MultipartResolver bean.
	 * Uses StandardServletMultipartResolver which relies on servlet multipart configuration
	 * specified in application.properties (spring.servlet.multipart.*).
	 * 
	 * This resolver properly handles:
	 * - PDF files
	 * - Image files (PNG, JPG, etc.)
	 * - Text files
	 * 
	 * @return MultipartResolver bean for processing multipart requests
	 */
	@Bean
	public MultipartResolver multipartResolver() {
		return new StandardServletMultipartResolver();
	}
}
