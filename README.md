# RiskPilot - Phishing Risk & Fraud Detection System

<div align="center">

![RiskPilot Logo](https://img.shields.io/badge/RiskPilot-Backend-blue?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.11-green?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-purple?style=flat-square)

**A comprehensive AI-powered system to detect phishing, scams, and fraudulent content in job descriptions, URLs, emails, and documents.**

[Features](#features) • [Tech Stack](#tech-stack) • [Setup](#setup) • [API Documentation](#api-documentation) • [Contributing](#contributing)

</div>

---

## 📋 Overview

RiskPilot is an intelligent fraud detection platform that uses advanced AI (Google Gemini) combined with traditional security analysis techniques to identify phishing attempts, scam job postings, and fraudulent content. The system analyzes:

- **URLs** - Domain age, SSL validity, redirect detection, malware checking
- **Emails** - Phishing pattern detection, domain reputation, lookalike domain detection
- **Files** - PDF, images (with OCR), and text files for hidden phishing content
- **Job Descriptions** - Comprehensive analysis combining AI and URL/email verification

The backend is built with Spring Boot 3.5.11 and integrates with Google Gemini AI for intelligent content analysis.

---

## ✨ Features

### 🔐 Security & Authentication
- **JWT-based Authentication** - Secure token-based user sessions
- **OAuth2 Integration** - Google OAuth2 for seamless login
- **User Registration & Password Management** - Secure credential handling
- **Email Verification** - Account verification workflows

### 🔍 Analysis Capabilities

#### URL Analysis
- Domain age verification
- SSL/TLS certificate validation
- Redirect chain detection
- DNS resolution and MX record validation
- Malware and phishing database checks
- Typosquatting detection
- URL pattern analysis
- **Trust Score & Risk Level** classification

#### Email Analysis
- Phishing pattern detection using Levenshtein algorithm
- Free email provider detection
- Domain reputation classification
- Lookalike domain identification
- Suspicious keyword detection
- **Confidence Level** assessment

#### File Scanning
- PDF text extraction (Apache PDFBox)
- Image-to-text OCR (Tess4j)
- Text file analysis (.txt, .csv, .log, .md)
- Embedded URL and email detection
- AI-powered content analysis

#### Job Description Analysis
- Automatic email and URL extraction
- Multi-stage verification pipeline
- Combined AI and traditional security checks
- Company verification
- Comprehensive risk assessment

### 📊 History & Analytics
- Scan history with search and filtering
- Risk statistics and trends
- Export capabilities
- User-specific history tracking
- Advanced filtering by risk level, date, and type

### 🤖 AI Integration
- **Google Gemini AI** integration for intelligent analysis
- Natural language processing for context understanding
- Scam pattern recognition
- Risk assessment and suggestions generation

---

## 🛠 Tech Stack

### Backend
- **Framework**: Spring Boot 3.5.11
- **Java Version**: JDK 17
- **Build Tool**: Maven
- **Security**: Spring Security, JWT (JJWT 0.13.0), OAuth2

### Database
- **Primary DB**: PostgreSQL
- **ORM**: Hibernate JPA

### External APIs & Services
- **AI Analysis**: Google Gemini API
- **Malware Detection**: VirusTotal API
- **OCR**: Tess4j 5.13.0
- **PDF Processing**: Apache PDFBox 3.0.4
- **DNS Resolution**: dnsjava 3.5.2

### Libraries & Dependencies
- **Lombok** - Boilerplate code reduction
- **Spring Data JPA** - Data persistence
- **Spring Security** - Authentication & authorization
- **Jackson** - JSON processing

---

## 📦 Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17** or higher
- **Maven 3.6+**
- **PostgreSQL 12+**
- **Git**

### Required API Keys

1. **Google Gemini API Key**
   - Get it from: https://aistudio.google.com/app/apikeys
   - Free tier available (60 requests/minute)

2. **VirusTotal API Key**
   - Get it from: https://www.virustotal.com/
   - Sign up for free account

3. **Google OAuth2 Credentials** (Optional)
   - Get from: https://console.cloud.google.com/
   - Required for OAuth2 login feature

---

## 🚀 Setup & Installation

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/riskPilot_backend.git
cd riskPilot_backend
```

### 2. Configure Environment Variables

Create a new file `src/main/resources/application.properties` with your configuration:

```properties
# Server Configuration
server.port=8080
spring.application.name=RiskPilot

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/riskPilot
spring.datasource.username=postgres
spring.datasource.password=YOUR_DB_PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver

# Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Gemini API Configuration
spring.ai.google.ai.gemini.api-key=YOUR_GEMINI_API_KEY
spring.ai.google.ai.gemini.model=gemini-2-flash

# VirusTotal API Configuration
virustotal.api.key=YOUR_VIRUSTOTAL_API_KEY

# OAuth2 Configuration (Google)
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
spring.security.oauth2.client.registration.google.scope=profile,email
spring.security.oauth2.client.registration.google.redirect-uri=http://localhost:8080/login/oauth2/code/google

# JWT Configuration
jwt.secret.key=YOUR_SUPER_SECRET_JWT_KEY_AT_LEAST_256_BITS_LONG

# File Upload Configuration
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

### 3. Create PostgreSQL Database

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE riskPilot;
```

### 4. Build the Project

```bash
mvn clean install
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

---

## 📡 API Documentation

### Base URL
```
http://localhost:8080/api/scan
```

### 1. Analyze URL

**Endpoint**: `POST /api/scan/url`

**Request Body**:
```json
{
  "url": "https://suspicious-link.com",
  "knownCompanyDomain": "amazon.com",
  "senderEmail": "recruiter@suspicious.com",
  "companyName": "Amazon Careers"
}
```

**Response**:
```json
{
  "riskLevel": "HIGH",
  "trustScore": 2.5,
  "domainAgeDays": 12,
  "sslValid": true,
  "redirectDetected": false,
  "malwareFlagged": true,
  "suspiciousIndicators": [
    "Domain age is suspicious (less than 30 days old)",
    "Malware detected by VirusTotal"
  ],
  "suggestions": [
    "🚨 HIGH RISK - Do not click this link",
    "Report to your email provider"
  ],
  "analysisSummary": "This URL shows multiple red flags..."
}
```

### 2. Analyze Email

**Endpoint**: `POST /api/scan/email`

**Request Body**:
```json
{
  "email": "hr@xyz-careers-job.com"
}
```

**Response**:
```json
{
  "riskLevel": "HIGH",
  "trustScore": 15,
  "riskScore": 85,
  "email": "hr@xyz-careers-job.com",
  "emailDomain": "xyz-careers-job.com",
  "isFreeEmailProvider": false,
  "domainType": "SUSPICIOUS",
  "isLookalikeDomain": true,
  "phishingSimilarityScore": 82,
  "suspiciousIndicators": [
    "Email uses custom domain with no verified reputation",
    "Phishing pattern detected (score: 82%)"
  ],
  "suggestions": [
    "⚠ Domain resembles a legitimate company (possible phishing)",
    "🚨 HIGH RISK - This appears to be a phishing attempt"
  ],
  "confidenceLevel": "HIGH"
}
```

### 3. Scan File

**Endpoint**: `POST /api/scan/file`

**Request**: multipart/form-data with key `file`

**Supported Formats**:
- PDF files (.pdf)
- Images (.png, .jpg, .jpeg, .tiff, .bmp, .gif)
- Text files (.txt, .csv, .log, .md)

**Response**:
```json
{
  "riskLevel": "MEDIUM",
  "trustScore": 5,
  "suspiciousIndicators": [
    "Potential phishing content detected",
    "Suspicious URL pattern found"
  ],
  "suggestions": [
    "Review the document carefully",
    "Verify any sender information"
  ],
  "analysisSummary": "The document contains suspicious patterns..."
}
```

### 4. Analyze Job Description

**Endpoint**: `POST /api/scan/description`

**Request Body**:
```json
{
  "jobDescription": "Full job description text here..."
}
```

**Response**:
```json
{
  "riskLevel": "HIGH",
  "trustScore": 3,
  "email": "hr@suspicious-company.com",
  "url": "https://suspicious-job.com",
  "emailVerified": false,
  "companyVerified": false,
  "suspiciousIndicators": [
    "Email domain has phishing patterns",
    "URL shows malware indicators"
  ],
  "suggestions": [
    "Do not respond to this job posting",
    "Report to the job platform"
  ],
  "analysisSummary": "This job posting shows multiple red flags..."
}
```

---

## 🔑 Authentication

### Register New User

**Endpoint**: `POST /api/auth/register`

### Login with Email & Password

**Endpoint**: `POST /api/auth/login`

### OAuth2 Google Login

**Endpoint**: `GET /oauth2/authorization/google`

All protected endpoints require JWT token in Authorization header:

```
Authorization: Bearer YOUR_JWT_TOKEN
```

---

## 📁 Project Structure

```
RiskPilot/
├── src/main/java/com/riskpilot/
│   ├── controller/          # REST API Controllers
│   ├── service/             # Business Logic
│   ├── model/               # Entity Classes & DTOs
│   ├── repository/          # Data Access Layer
│   ├── security/            # Security Configuration
│   ├── util/                # Utility Classes
│   └── RiskPilotApplication.java
├── src/main/resources/
│   ├── application.properties  # Configuration (excluded from Git)
│   └── db/                     # Database scripts
├── pom.xml                  # Maven Dependencies
├── .gitignore               # Git Ignore Rules
└── README.md               # Documentation
```

---

## 🔒 Security Considerations

### Sensitive Files (Excluded from Repository)

The following files are excluded from version control and should never be committed:

- `src/main/resources/application.properties` - Contains API keys and credentials
- `.env` files - Environment variables
- `*.key`, `*.pem` files - Private keys
- `credentials.json` - API credentials

### Best Practices

1. **Always use environment variables** for sensitive data
2. **Never commit API keys** or credentials to the repository
3. **Use HTTPS** for all API communications
4. **Rotate JWT secrets** periodically in production
5. **Keep dependencies updated** to patch security vulnerabilities
6. **Implement rate limiting** to prevent API abuse
7. **Log security events** for audit trails

---

## 🧪 Testing

Run unit tests with Maven:

```bash
mvn test
```

Run integration tests:

```bash
mvn verify
```

---

## 📝 Configuration Examples

### PostgreSQL Connection String

For local development:
```
jdbc:postgresql://localhost:5432/riskPilot
```

For production (consider using environment variables):
```
jdbc:postgresql://prod-db.example.com:5432/riskPilot
```

### Gemini API Configuration

The application uses Google's Gemini 2.0 Flash model by default. You can change the model in application.properties:

```properties
spring.ai.google.ai.gemini.model=gemini-2-flash
```

---

## 🚀 Deployment

### Build Production JAR

```bash
mvn clean package -DskipTests
```

### Run as Service

```bash
java -jar target/RiskPilot-0.0.1-SNAPSHOT.jar
```

### Docker Deployment (Optional)

Create a `Dockerfile`:

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/RiskPilot-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:

```bash
docker build -t riskpilot:latest .
docker run -d -p 8080:8080 riskpilot:latest
```

---

## 📊 Architecture Highlights

### Multi-Tier Architecture
- **Presentation Tier**: React frontend (separate repository)
- **Application Tier**: Spring Boot REST APIs
- **Data Tier**: PostgreSQL with Hibernate ORM

### Service Layer
- **AnalysisService** - Core file analysis
- **UrlAnalysisService** - URL verification engine
- **EmailAnalysisService** - Email pattern detection
- **DescriptionAnalysisService** - Job description analysis
- **HistoryService** - Scan history management
- **AuthenticationService** - User authentication

### Integration Points
- **Google Gemini API** - AI-powered content analysis
- **VirusTotal API** - Malware database checking
- **DNS Resolution** - Domain validation
- **OAuth2 Provider** - Google authentication

---

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Style
- Follow Google Java Style Guide
- Use meaningful variable names
- Add JavaDoc comments for public methods
- Write unit tests for new features

---

## 📚 Documentation

For more detailed documentation:
- See [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md) - Project implementation details
- See [SYSTEM_DESIGN.md](./SYSTEM_DESIGN.md) - System architecture and design
- See [POSTMAN_GUIDE.txt](./POSTMAN_GUIDE.txt) - API testing with Postman

---

## 🐛 Known Issues & Limitations

1. **File Size Limit**: Maximum 10MB per file upload
2. **OCR Accuracy**: Text extraction from images depends on image quality
3. **API Rate Limits**: 
   - Gemini API: 60 requests/minute (free tier)
   - VirusTotal API: Rate limits based on plan
4. **GDPR Compliance**: Ensure proper data handling for EU users

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 👥 Authors

- **Your Name** - Project Lead & Developer

---

## 💬 Support & Contact

For issues, questions, or suggestions:

- 📧 Email: support@riskpilot.com
- 🐛 Issue Tracker: GitHub Issues
- 📖 Documentation: See docs/ folder

---

## 🙏 Acknowledgments

- Google Gemini AI - For advanced content analysis
- VirusTotal - For malware database
- Spring Framework - For excellent framework
- PostgreSQL - For reliable database

---

<div align="center">

**Made with ❤️ for security and fraud detection**

[⬆ back to top](#riskpilot---phishing-risk--fraud-detection-system)

</div>
