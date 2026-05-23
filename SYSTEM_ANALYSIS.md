# CHAPTER 3: SYSTEM ANALYSIS
## RiskPilot - Phishing Risk & Fraud Detection System

---

## 3.1 Problem Description

### 3.1.1 Problem Definition

The RiskPilot system addresses critical challenges in detecting and mitigating phishing attacks, fraud indicators, and job scams. Below are the key problems identified:

#### 1. **Increasing Phishing Attack Threats**
- Phishing attacks have become increasingly sophisticated and harder to detect manually.
- Users lack automated tools to analyze suspicious URLs and emails in real-time.
- False positives and false negatives in manual detection lead to security breaches.
- Organizations struggle to implement effective anti-phishing measures.

#### 2. **Lack of Real-Time URL Analysis**
- No centralized platform exists to analyze URLs for phishing indicators instantly.
- Users must rely on external tools scattered across different platforms.
- Manual URL verification is time-consuming and prone to human error.
- Legitimate URLs are sometimes flagged incorrectly, creating user friction.

#### 3. **Email-Based Threat Detection Challenges**
- Malicious emails bypass traditional spam filters and reach users' inboxes.
- Email header analysis requires specialized technical knowledge.
- No simple interface exists for non-technical users to verify email authenticity.
- Spoofed sender addresses and fake domains are hard to identify.

#### 4. **Ineffective File Security Scanning**
- Organizations lack unified file scanning solutions for document analysis.
- Malware detection is not standardized across departments.
- File upload processes lack automated security checks.
- Users cannot quickly verify if downloaded files are safe.

#### 5. **Job Scam Detection Gaps**
- Job seekers are vulnerable to fraudulent job postings and offers.
- Red flags in job descriptions (salary inconsistencies, suspicious requirements) are not automatically identified.
- No AI-powered system exists to analyze job listings for scam indicators.
- Legitimate job seekers lose time and money to job scams.

#### 6. **Lack of Centralized Scan History & Analytics**
- Users have no persistent record of previously analyzed content.
- Organizations cannot track security incidents or generate compliance reports.
- Risk assessment trends are invisible without centralized logging.
- No historical data to identify patterns or recurring threats.

#### 7. **Limited User Control & Accessibility**
- Different security tools require separate logins and interfaces.
- Non-technical users struggle to use complex security platforms.
- Mobile and desktop access is fragmented.
- No unified dashboard to manage all security analyses.

#### 8. **Poor Authentication & Data Privacy**
- Sensitive scan results and user data lack proper encryption.
- No secure authentication mechanisms in existing tools.
- Unauthorized access to security reports is a major risk.
- User privacy is compromised when data is shared across multiple platforms.

---

### 3.1.2 Proposed Solution

#### **Comprehensive Risk & Threat Detection Platform (RiskPilot)**

RiskPilot is an integrated, AI-powered security analysis platform designed to address all identified problems through the following solutions:

#### 1. **Centralized URL Analysis Engine**
- Automated real-time scanning of URLs for phishing, malware, and suspicious indicators.
- Analysis includes domain reputation, SSL validation, phishing database comparison, redirect chains.
- Instant risk scoring with color-coded severity levels.
- Visual reports showing detailed threat indicators.

#### 2. **Intelligent Email Analysis System**
- Complete email header parsing and authentication verification.
- Checks for SPF, DKIM, DMARC protocol compliance and sender spoofing detection.
- Domain legitimacy verification and embedded URL analysis.
- Provides actionable insights for identifying phishing emails.

#### 3. **Advanced File Security Scanning**
- Multi-layered file analysis for malware patterns, integrity, and suspicious components.
- Supports common file formats (PDF, DOC, EXE, ZIP, etc.).
- Generates detailed file scan reports with remediation suggestions.
- Quarantine recommendations for high-risk files.

#### 4. **AI-Powered Job Description Analysis**
- Automatic scanning of job descriptions for scam indicators.
- Uses **Google Gemini AI** to identify fraudulent vs. legitimate job postings.
- Detects red flags: unrealistic salary, vague responsibilities, pressure tactics.
- Provides guidance and safety recommendations to job seekers.

#### 5. **Unified Scan History & Analytics Dashboard**
- Persistent storage of all analyses with advanced filtering.
- Filter by analysis type (URL, Email, File, Job) and risk level.
- Statistical reports showing threat trends and risk distribution.
- Export capabilities for compliance and auditing.

#### 6. **Secure Multi-Authentication System**
- Traditional username/password authentication with bcrypt hashing.
- OAuth 2.0 integration with Google for seamless login.
- JWT token-based session management for API security.
- Secure token refresh mechanism and role-based access control.

#### 7. **User-Friendly Unified Interface**
- Single dashboard for all security analyses.
- Intuitive UI/UX design accessible to non-technical users.
- Real-time results display with visual indicators.
- Mobile-responsive design for on-the-go threat detection.

#### 8. **Enterprise-Grade Data Security**
- End-to-end encryption for sensitive data.
- Secure PostgreSQL database with role-based access.
- HTTPS/TLS for all communications and CORS configuration.
- GDPR compliance and comprehensive audit logs.

#### 9. **AI-Powered User Assistance**
- Integrated **Google Gemini AI chatbot** for real-time security questions.
- Multi-turn conversations with context awareness.
- Guidance on identifying threats and security best practices.
- Educational content on cybersecurity awareness.

#### 10. **Admin Control & Monitoring Dashboard**
- Comprehensive admin panel for user management and verification.
- Monitoring system usage, performance, and user activities.
- Managing complaints, disputes, and system health.
- Performance analytics and role-based permissions.

---

## 3.2 Requirements

### 3.2.1 Functional Requirements

Functional requirements describe what the RiskPilot system should do – the features and functions:

1. **User Registration & Authentication**
   - Users can create accounts using email, password, or Google OAuth2 login.
   - Users can securely log in and log out.
   - Password encryption using bcrypt hashing.
   - JWT token generation and management for session handling.

2. **User Profile Management**
   - Users can create and update profiles with personal information (name, email, contact details).
   - Users can upload and manage profile pictures.
   - Users can view and edit their account settings.
   - Profile data is securely stored and accessible only to the user.

3. **URL Analysis & Threat Detection**
   - Users can input URLs for real-time phishing and malware analysis.
   - System performs domain reputation checking and SSL certificate verification.
   - System detects URL redirect chains and suspicious patterns.
   - System assigns risk scores (0-100) and provides detailed threat reports.
   - Analysis results are saved to user's history automatically.

4. **Email Security Analysis**
   - Users can input email headers for authentication verification.
   - System parses and analyzes SPF, DKIM, and DMARC protocols.
   - System detects sender spoofing and domain legitimacy issues.
   - System extracts and analyzes embedded URLs within emails.
   - System provides actionable security recommendations.

5. **File Security Scanning**
   - Users can upload files for malware and threat analysis.
   - System supports multiple file formats (PDF, DOC, EXE, ZIP, Images, etc.).
   - System performs malware pattern detection and file integrity verification.
   - System examines file metadata and suspicious components.
   - System generates detailed scan reports with risk levels and quarantine recommendations.

6. **Job Description Fraud Detection**
   - Users can input job descriptions for scam indicator analysis.
   - System uses Gemini AI to analyze job listings for fraud indicators.
   - System identifies red flags (unrealistic salary, suspicious requirements, pressure tactics).
   - System assigns fraud risk scores and provides safety guidance.
   - System highlights suspicious phrases and company legitimacy concerns.

7. **Scan History & Management**
   - System stores all user analyses (URLs, emails, files, jobs) with timestamps.
   - Users can view, search, and filter their complete scan history.
   - Users can filter scans by analysis type, risk level, or date range.
   - System displays high-risk scan summaries and threat statistics.
   - Users can delete individual scans or export history data for compliance.

8. **AI-Powered Chat Support**
   - Users can access 24/7 AI chat support powered by Google Gemini API.
   - System supports multi-turn conversations with context awareness.
   - AI provides security guidance, threat education, and result explanations.
   - AI explains analysis results in simple, understandable terms.
   - Chat interactions are saved for reference and continuous improvement.

9. **Admin Dashboard & Management**
   - Admins can view and manage user accounts with verification status.
   - Admins can monitor system usage, performance metrics, and user activities.
   - Admins can view threat statistics, analytics, and trend reports.
   - Admins can handle user disputes and manage access permissions.
   - Admins can perform system health monitoring and access audit logs.

10. **Data Security & Privacy**
    - All sensitive data is encrypted using AES-256 encryption.
    - All communications are secured with HTTPS/TLS protocols.
    - User data is protected with role-based access control (RBAC).
    - System maintains comprehensive audit trails of all user activities.
    - Compliance with GDPR and data privacy regulations.

---

### 3.2.2 Non-Functional Requirements

Non-functional requirements describe how the system performs or the quality attributes:

• **Usability**: The interface must be simple, intuitive, and accessible to users with minimal technical knowledge. Mobile-responsive design, clear navigation, guided onboarding, and helpful tooltips ensure ease of use across devices.

• **Performance**: The system should load dashboards and analysis results within acceptable response time limits. URL analysis < 2 seconds, email analysis < 3 seconds, file uploads < 5 seconds, dashboard load < 1.5 seconds, API responses < 500ms.

• **Security**: User data must be protected using standard security practices such as encryption (bcrypt for passwords, AES-256 for data), secure authentication (JWT tokens with 24-hour expiration), parameterized SQL queries, CORS restrictions, and rate limiting (100 requests per minute per user).

• **Scalability**: The system should handle increasing numbers of transactions and users without performance issues. Horizontal scaling with load balancing, PostgreSQL read replicas, Redis caching, cloud storage (AWS S3), support for 100,000+ concurrent users, and ability to handle 1 million+ scans.

• **Reliability**: The system must provide consistent results and stable performance with 99.5% uptime SLA. Daily automated backups, < 30-minute recovery time, < 1-hour data recovery, multi-region failover support, and health checks every 30 seconds.

• **Maintainability**: Code should be modular, well-documented, and easy to update or enhance. SonarQube A+ rating, 80% test coverage, comprehensive documentation, CI/CD pipeline automation, structured logging (ELK Stack), and Git version control with meaningful commits.

• **Accessibility**: Full WCAG 2.1 AA compliance for users with disabilities. Support for multiple languages, screen reader compatibility, keyboard navigation, and high contrast modes.

• **Data Integrity**: ACID compliance for critical operations, database integrity constraints, transaction consistency, and validation of all inputs to prevent data corruption.

---

## 3.3 Problem Analysis Diagrams

### 3.3.1 Data Flow Diagram (DFD)

#### **Level 0 - Context Diagram**

```
                           [External APIs]
                                 |
                    _____________|___________
                   |                         |
            [Google Gemini API]      [Threat Detection APIs]
                   |                         |
                   |
         ╔═════════════════════╗
         ║    RiskPilot        ║
         ║   Core System       ║
         ╚═════════════════════╝
              |        |        |
              |        |        |
         _____|________|________|_____
         |          |          |     |
    [Users]   [Database]  [Cache]  [Storage]
```

#### **Level 1 - Detailed DFD**

```
PROCESS 1: User Authentication
INPUT: User Credentials / OAuth Token
OUTPUT: JWT Token, Session ID

PROCESS 2: URL Analysis
INPUT: URL String
OUTPUT: Risk Score, Threat Details, Report

PROCESS 3: Email Analysis
INPUT: Email Headers
OUTPUT: Authentication Status, Threat Level, Recommendations

PROCESS 4: File Analysis
INPUT: File (Binary/Document)
OUTPUT: Scan Report, Risk Level, Recommendations

PROCESS 5: Job Description Analysis
INPUT: Job Description Text
OUTPUT: Risk Score, AI Analysis, Recommendations

PROCESS 6: History Management
INPUT: Scan Results
OUTPUT: Stored Scan, History View, Analytics

PROCESS 7: AI Chat Support
INPUT: User Query
OUTPUT: AI Response, Guidance

PROCESS 8: Admin Management
INPUT: Admin Commands
OUTPUT: Updated System State, Audit Log
```

---

### 3.3.2 Use Case Diagram

```
                    ┌─────────────────────────┐
                    │                         │
                    │    RiskPilot System     │
                    │                         │
                    └─────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
    [User]             [Premium User]          [Admin]
        │                     │                     │
        ├─── UC1: Register ───┤                     │
        ├─── UC2: Login ──────┤                     │
        ├─── UC3: Analyze URL ┤                     │
        ├─── UC4: Analyze Email──────────┐         │
        ├─── UC5: Analyze File ──────────┤         │
        ├─── UC6: Analyze Job Description┤         ├─── UC9: Manage Users
        ├─── UC7: View History ──────────┤         │
        ├─── UC8: Chat with AI ─────────┤         ├─── UC10: View Analytics
        │                               │         │
        │  Premium Features:           │         ├─── UC11: System Monitor
        │  ├─ Advanced Reports         │         │
        │  ├─ Batch Analysis           │         └──────────────────────
        │  └─ API Access              │
        │                               │
        └───────────────────────────────┘
```

---

### 3.3.3 Sequence Diagram

#### **Sequence 1: User Registration and Email Analysis**

```
User → Browser → API Backend → Database → Email Validator
 |        |           |            |           |
 |--Register-------->|            |           |
 |                   |--POST /register-->|     |
 |                   |                   |-----|
 |                   |<--Success---------|     |
 |<--Registered------|                   |     |
 |                   |                   |     |
 |--Login---------->|                   |     |
 |                   |--POST /login----->|     |
 |                   |<--JWT Token------|     |
 |<--Logged In-------|                   |     |
 |                   |                   |     |
 |--Analyze Email-->|                   |     |
 |                   |-POST /analyze/email    |
 |                   |--Parse Headers-------->|
 |                   |<--SPF/DKIM Valid-----|
 |                   |--Store Result------|   |
 |                   |<--Risk Report------|   |
 |<--Analysis Result-|                   |     |
```

#### **Sequence 2: URL Analysis with Risk Detection**

```
User → Browser → API Backend → Cache → Risk Engine
 |        |           |         |          |
 |--Analyze URL---->|         |          |
 |                  |--POST /analyze/url |
 |                  |--Check Cache------>|
 |                  |<--Cache Hit (if)---|
 |                  |                    |
 |                  |--Analyze Domain----------|
 |                  |--Check URL Pattern-------|
 |                  |<--Risk Score (0-100)-----|
 |                  |--Cache Result---->|      |
 |                  |<--Detailed Report-|      |
 |<--Results Display-|                 |      |
```

#### **Sequence 3: Job Analysis with Gemini AI**

```
User → Browser → API Backend → Database → Gemini API
 |        |           |            |           |
 |--Analyze Job---->|            |           |
 |                  |--POST /analyze/job      |
 |                  |--Validate Input--------|
 |                  |--Store Job Text--------|
 |                  |--Prepare Prompt-------|---------|
 |                  |               |       |<--AI Analysis
 |                  |<--Parse Response------|
 |                  |--Store Result--------|
 |                  |<--Risk Report--------|
 |<--Analysis Display-|                   |           |
```

---

## 3.4 Database Schema

### **PostgreSQL Database Design**

#### **3.4.1 Users Table**

```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    profile_picture_url VARCHAR(255),
    user_role VARCHAR(20) DEFAULT 'USER',
    auth_provider VARCHAR(50) DEFAULT 'LOCAL',
    google_id VARCHAR(255) UNIQUE,
    is_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    is_premium BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);
```

#### **3.4.2 Scans Table** (Unified History)

```sql
CREATE TABLE scans (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scan_type VARCHAR(50) NOT NULL,
    url VARCHAR(2048),
    url_risk_score DECIMAL(5, 2),
    email_from VARCHAR(255),
    email_subject VARCHAR(255),
    file_name VARCHAR(255),
    file_size BIGINT,
    job_title VARCHAR(255),
    job_description TEXT,
    job_risk_score DECIMAL(5, 2),
    ai_recommendation TEXT,
    result_summary TEXT,
    detailed_report JSONB,
    risk_level VARCHAR(20),
    status VARCHAR(20) DEFAULT 'COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### **3.4.3 Sessions Table**

```sql
CREATE TABLE sessions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) UNIQUE NOT NULL,
    refresh_token VARCHAR(500) UNIQUE NOT NULL,
    ip_address VARCHAR(45),
    expires_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### **3.4.4 Chat Messages Table**

```sql
CREATE TABLE chat_messages (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_text TEXT NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    conversation_id VARCHAR(255),
    response TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### **3.4.5 Admin Logs Table**

```sql
CREATE TABLE admin_logs (
    id SERIAL PRIMARY KEY,
    admin_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action_type VARCHAR(100) NOT NULL,
    target_user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    action_details JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

### **3.4.6 Database Relationships**

```
                    ┌──────────────────┐
                    │     Users        │
                    ├──────────────────┤
                    │ id (PK)          │
                    │ username         │
                    │ email            │
                    │ password_hash    │
                    │ user_role        │
                    │ is_premium       │
                    └────────┬─────────┘
                             │ (1:N)
                    ┌────────┴──────────────────────┐
                    │                               │
        ┌───────────▼──────────┐    ┌──────────────▼────────┐
        │     Scans            │    │   Sessions           │
        ├──────────────────────┤    ├──────────────────────┤
        │ id (PK)              │    │ id (PK)              │
        │ user_id (FK)         │    │ user_id (FK)         │
        │ scan_type            │    │ token                │
        │ url/email/file/job   │    │ refresh_token        │
        │ risk_level           │    │ expires_at           │
        │ created_at           │    └──────────────────────┘
        └────────┬─────────────┘
                 │                 ┌──────────────────────┐
                 │                 │ Chat Messages        │
                 │                 ├──────────────────────┤
                 │                 │ id (PK)              │
                 │                 │ user_id (FK)         │
                 │                 │ message_text         │
                 │                 │ response             │
                 │                 │ created_at           │
                 │                 └──────────────────────┘
                 │
        ┌────────▼──────────────┐
        │  Admin Logs          │
        ├──────────────────────┤
        │ id (PK)              │
        │ admin_id (FK)        │
        │ action_type          │
        │ target_user_id (FK)  │
        │ created_at           │
        └──────────────────────┘
```

---

## Summary

This System Analysis chapter provides:

1. **Problem Definition**: Identified 8 major challenges in phishing/fraud detection
2. **Proposed Solution**: Comprehensive 10-point solution addressing all problems
3. **Functional Requirements**: 10 detailed functional requirements covering all features
4. **Non-Functional Requirements**: 8 quality attributes (Usability, Performance, Security, Scalability, Reliability, Maintainability, Accessibility, Data Integrity)
5. **Problem Analysis Diagrams**: DFD, Use Cases, and Sequence Diagrams
6. **Database Schema**: Complete PostgreSQL schema with 5 main tables and relationships

---

**Document Version**: 1.0  
**Last Updated**: May 2026  
**Status**: Complete
