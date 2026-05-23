# CHAPTER 4: SYSTEM DESIGN
## RiskPilot - Phishing Risk & Fraud Detection System

---

## 4.1 System Architecture

### **High-Level Architecture Diagram**

```
┌─────────────────────────────────────────────────────────────────┐
│                        PRESENTATION TIER                        │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ Web Frontend (React + Material-UI)                      │  │
│  │ ├── Login/Registration Interface                        │  │
│  │ ├── URL Analysis Dashboard                             │  │
│  │ ├── Email Analysis Form                                │  │
│  │ ├── File Upload Scanner                                │  │
│  │ ├── Job Description Analyzer                           │  │
│  │ ├── Scan History & Filter                              │  │
│  │ ├── AI Chat Interface                                  │  │
│  │ └── Admin Dashboard & User Management                  │  │
│  └─────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────────┘
                         │ (HTTPS REST APIs)
┌────────────────────────▼────────────────────────────────────────┐
│              APPLICATION TIER (Spring Boot)                     │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ Authentication Service                                  │  │
│  │ ├── User Registration (Email/Password & OAuth2)        │  │
│  │ ├── JWT Token Generation & Validation                  │  │
│  │ ├── Session Management                                 │  │
│  │ └── Password Reset & Email Verification                │  │
│  └─────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ Analysis Services                                       │  │
│  │ ├── URL Analysis Engine                                │  │
│  │ ├── Email Header Parser & Validator                    │  │
│  │ ├── File Scan Service                                  │  │
│  │ └── Job Description Analyzer (Gemini AI Integration)   │  │
│  └─────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ History & Analytics Service                            │  │
│  │ ├── Scan Storage & Retrieval                           │  │
│  │ ├── Search & Filtering Engine                          │  │
│  │ ├── Statistics & Report Generation                     │  │
│  │ └── Export & Compliance Reports                        │  │
│  └─────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ AI Chat Service (Gemini Integration)                   │  │
│  │ ├── Message Processing                                 │  │
│  │ ├── Conversation Context Management                    │  │
│  │ └── Response Generation                                │  │
│  └─────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ Admin Service                                           │  │
│  │ ├── User Management                                    │  │
│  │ ├── System Monitoring                                  │  │
│  │ ├── Audit Log Management                               │  │
│  │ └── Report Generation                                  │  │
│  └─────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────────┘
                         │ (Database Access via JPA/Hibernate)
┌────────────────────────▼────────────────────────────────────────┐
│                       DATA TIER                                 │
│  ┌──────────────────────┐     ┌──────────────────────┐         │
│  │   PostgreSQL DB      │     │  Redis Cache         │         │
│  │                      │     │                      │         │
│  │ • Users              │     │ • Session Tokens     │         │
│  │ • Scans              │     │ • User Cache         │         │
│  │ • Sessions           │     │ • Frequent Queries   │         │
│  │ • Chat Messages      │     │ • Analysis Results   │         │
│  │ • Admin Logs         │     │ • Analytics Data     │         │
│  └──────────────────────┘     └──────────────────────┘         │
│                                                                 │
│  ┌──────────────────────┐     ┌──────────────────────┐         │
│  │  File Storage (S3)   │     │  External APIs       │         │
│  │                      │     │                      │         │
│  │ • Uploaded Files     │     │ • Google Gemini      │         │
│  │ • Scan Reports       │     │ • OAuth2 Provider    │         │
│  │ • User Documents     │     │ • Threat DB APIs     │         │
│  └──────────────────────┘     └──────────────────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

### **Architecture Layers Description**

#### **1. Presentation Tier (Frontend)**
- **Technology**: React.js, Material-UI, JavaScript
- **Responsibilities**: User interface, form validation, real-time data display
- **Components**: 
  - Authentication pages (Login/Register)
  - Analysis input forms (URL, Email, File, Job)
  - Results display with visual indicators
  - History and analytics dashboards
  - AI chat interface
  - Admin control panel

#### **2. Application Tier (Backend)**
- **Technology**: Spring Boot, Java 17+
- **Responsibilities**: Business logic, API endpoints, data validation, service orchestration
- **Key Services**:
  - **AuthenticationService**: Handles user registration, login, JWT token management
  - **URLAnalysisService**: Performs URL threat analysis
  - **EmailAnalysisService**: Parses and validates email headers
  - **FileAnalysisService**: Scans uploaded files for threats
  - **JobAnalysisService**: AI-powered job scam detection
  - **HistoryService**: Manages scan records and retrieval
  - **ChatService**: Integrates Gemini AI for user assistance
  - **AdminService**: User management and system monitoring
  - **CacheService**: Redis integration for performance optimization

#### **3. Data Tier**
- **Primary Database**: PostgreSQL (relational data storage)
- **Cache Layer**: Redis (session tokens, frequent queries, analytics)
- **File Storage**: AWS S3 or Google Cloud Storage (uploaded files, reports)
- **External APIs**: Google Gemini, OAuth2, Threat Detection services

---

## 4.2 Physical Design

### **4.2.1 Component Diagram**

```
                        ┌─────────────────────┐
                        │   Frontend (React)   │
                        └──────────┬───────────┘
                                   │
                    ┌──────────────┼──────────────┐
                    │              │              │
        ┌───────────▼───────┐   ┌──▼──────────┐   │
        │ REST API Gateway  │   │ WebSocket   │   │
        │ (Spring REST)     │   │ (Chat)      │   │
        └───────────┬───────┘   └──┬──────────┘   │
                    │              │              │
        ┌───────────┴──────────────┴──────────┐   │
        │   Application Services Layer        │   │
        │ ┌──────────────────────────────┐  │   │
        │ │ Authentication Controller    │  │   │
        │ │ Analysis Controllers         │  │   │
        │ │ History Controller           │  │   │
        │ │ Chat Controller              │  │   │
        │ │ Admin Controller             │  │   │
        │ └──────────────────────────────┘  │   │
        └────────────┬─────────────────────────┘   │
                     │                              │
        ┌────────────┴──────────────────────────┐  │
        │   Service Layer                       │  │
        │ ┌──────────────────────────────────┐ │  │
        │ │ AuthenticationService            │ │  │
        │ │ URLAnalysisService               │ │  │
        │ │ EmailAnalysisService             │ │  │
        │ │ FileAnalysisService              │ │  │
        │ │ JobAnalysisService               │ │  │
        │ │ HistoryService                   │ │  │
        │ │ ChatService                      │ │  │
        │ │ AdminService                     │ │  │
        │ │ CacheService                     │ │  │
        │ └──────────────────────────────────┘ │  │
        └────────────┬──────────────────────────┘  │
                     │                              │
        ┌────────────┴──────────────────────────┐  │
        │   Repository/DAO Layer               │  │
        │ ┌──────────────────────────────────┐ │  │
        │ │ UserRepository                   │ │  │
        │ │ ScanRepository                   │ │  │
        │ │ SessionRepository                │ │  │
        │ │ ChatMessageRepository            │ │  │
        │ │ AdminLogRepository               │ │  │
        │ └──────────────────────────────────┘ │  │
        └────────────┬──────────────────────────┘  │
                     │                              │
        ┌────────────┴──────────────────────────┐  │
        │   Data Access Layer                  │  │
        │ ┌──────────────────────────────────┐ │  │
        │ │ PostgreSQL Database              │ │  │
        │ │ Redis Cache                      │ │  │
        │ │ S3 File Storage                  │ │  │
        │ │ External APIs                    │ │  │
        │ └──────────────────────────────────┘ │  │
        └────────────────────────────────────────┘  │
```

### **4.2.2 Entity-Relationship Diagram (ER Diagram)**

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│                    USERS                               │
│  ┌─────────────────────────────────────────────────┐  │
│  │ PK: id                                          │  │
│  │ username (UNIQUE)                               │  │
│  │ email (UNIQUE)                                  │  │
│  │ password_hash                                   │  │
│  │ first_name, last_name                           │  │
│  │ user_role (USER, ADMIN)                         │  │
│  │ auth_provider (LOCAL, GOOGLE)                   │  │
│  │ is_verified, is_active, is_premium              │  │
│  │ created_at, updated_at                          │  │
│  └──────────────────┬───────────────────────────────┘  │
│                     │ 1:N                               │
├─────────────────────┼───────────────────────────────────┤
│                     │                                   │
│  ┌──────────────────▼─────────────┐                   │
│  │        SCANS (History)         │                   │
│  │ ┌───────────────────────────┐  │                   │
│  │ │ PK: id                    │  │                   │
│  │ │ FK: user_id               │  │                   │
│  │ │ scan_type (URL/EMAIL/FILE/JOB) │              │
│  │ │ url, email_from, file_name, job_title │      │
│  │ │ risk_level (LOW/MEDIUM/HIGH) │ │             │
│  │ │ result_summary, detailed_report │ │          │
│  │ │ created_at, updated_at    │  │                   │
│  │ └───────────────────────────┘  │                   │
│  └──────────────────────────────────┘                   │
│                                                         │
│  ┌──────────────────────────┐                          │
│  │    SESSIONS              │                          │
│  │ ┌──────────────────────┐ │                          │
│  │ │ PK: id               │ │                          │
│  │ │ FK: user_id          │ │                          │
│  │ │ token (UNIQUE)       │ │                          │
│  │ │ refresh_token        │ │                          │
│  │ │ expires_at           │ │                          │
│  │ │ is_active            │ │                          │
│  │ │ created_at           │ │                          │
│  │ └──────────────────────┘ │                          │
│  └──────────────────────────┘                          │
│                                                         │
│  ┌──────────────────────────┐                          │
│  │   CHAT_MESSAGES          │                          │
│  │ ┌──────────────────────┐ │                          │
│  │ │ PK: id               │ │                          │
│  │ │ FK: user_id          │ │                          │
│  │ │ message_text         │ │                          │
│  │ │ message_type         │ │                          │
│  │ │ conversation_id      │ │                          │
│  │ │ response             │ │                          │
│  │ │ created_at           │ │                          │
│  │ └──────────────────────┘ │                          │
│  └──────────────────────────┘                          │
│                                                         │
│  ┌──────────────────────────┐                          │
│  │   ADMIN_LOGS             │                          │
│  │ ┌──────────────────────┐ │                          │
│  │ │ PK: id               │ │                          │
│  │ │ FK: admin_id         │ │                          │
│  │ │ action_type          │ │                          │
│  │ │ target_user_id       │ │                          │
│  │ │ action_details       │ │                          │
│  │ │ created_at           │ │                          │
│  │ └──────────────────────┘ │                          │
│  └──────────────────────────┘                          │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### **4.2.3 Class Diagram**

```
┌────────────────────────────────────┐
│           User Entity               │
├────────────────────────────────────┤
│ - id : Long                         │
│ - username : String                 │
│ - email : String                    │
│ - passwordHash : String             │
│ - firstName : String                │
│ - lastName : String                 │
│ - profilePictureUrl : String        │
│ - userRole : UserRole               │
│ - authProvider : AuthProvider       │
│ - googleId : String                 │
│ - isVerified : Boolean              │
│ - isActive : Boolean                │
│ - isPremium : Boolean               │
│ - createdAt : LocalDateTime         │
│ - updatedAt : LocalDateTime         │
│ - lastLogin : LocalDateTime         │
├────────────────────────────────────┤
│ + register() : void                 │
│ + login() : JWT                     │
│ + logout() : void                   │
│ + updateProfile() : void            │
│ + getScans() : List<Scan>          │
└────────────────────────────────────┘
         │ 1
         │ has many
         ▼ N
┌────────────────────────────────────┐
│         Scan Entity                 │
├────────────────────────────────────┤
│ - id : Long                         │
│ - user : User                       │
│ - scanType : ScanType               │
│ - url : String                      │
│ - urlRiskScore : BigDecimal         │
│ - emailFrom : String                │
│ - emailSubject : String             │
│ - fileName : String                 │
│ - fileSize : Long                   │
│ - jobTitle : String                 │
│ - jobDescription : String           │
│ - jobRiskScore : BigDecimal         │
│ - riskLevel : RiskLevel             │
│ - resultSummary : String            │
│ - detailedReport : String (JSON)    │
│ - status : ScanStatus               │
│ - createdAt : LocalDateTime         │
│ - updatedAt : LocalDateTime         │
├────────────────────────────────────┤
│ + analyzeURL() : void               │
│ + analyzeEmail() : void             │
│ + analyzeFile() : void              │
│ + analyzeJob() : void               │
│ + getReport() : String              │
│ + saveToHistory() : void            │
└────────────────────────────────────┘

┌────────────────────────────────────┐
│     AuthenticationService           │
├────────────────────────────────────┤
│ - userRepository : UserRepository   │
│ - passwordEncoder : BCryptEncoder   │
│ - jwtTokenProvider : JWTProvider    │
├────────────────────────────────────┤
│ + register() : User                 │
│ + login() : AuthResponse            │
│ + refreshToken() : JWT              │
│ + validateToken() : Boolean         │
│ + logout() : void                   │
└────────────────────────────────────┘

┌────────────────────────────────────┐
│     URLAnalysisService              │
├────────────────────────────────────┤
│ - threatDbClient : ThreatDB         │
│ - domainAnalyzer : DomainAnalyzer   │
│ - sslValidator : SSLValidator       │
├────────────────────────────────────┤
│ + analyzeURL(url) : AnalysisResult  │
│ + checkDomainReputation() : Score   │
│ + validateSSL() : Boolean           │
│ + detectRedirects() : List<URL>     │
│ + generateReport() : String         │
└────────────────────────────────────┘

┌────────────────────────────────────┐
│    JobAnalysisService               │
├────────────────────────────────────┤
│ - geminiClient : GeminiAPI          │
│ - scamDetector : ScamDetector       │
├────────────────────────────────────┤
│ + analyzeJob() : AnalysisResult     │
│ + detectScamIndicators() : List     │
│ + getAIRecommendation() : String    │
│ + calculateRiskScore() : Score      │
└────────────────────────────────────┘

┌────────────────────────────────────┐
│     HistoryService                  │
├────────────────────────────────────┤
│ - scanRepository : ScanRepository   │
│ - cacheService : CacheService       │
├────────────────────────────────────┤
│ + saveScan() : Scan                 │
│ + getUserScans() : List<Scan>       │
│ + filterScans() : List<Scan>        │
│ + deleteScan() : void               │
│ + generateStatistics() : Stats      │
│ + exportHistory() : File            │
└────────────────────────────────────┘

┌────────────────────────────────────┐
│     ChatService                     │
├────────────────────────────────────┤
│ - geminiAPI : GeminiAPI             │
│ - chatRepository : ChatRepository   │
│ - conversationManager : Manager     │
├────────────────────────────────────┤
│ + sendMessage() : ChatResponse      │
│ + getResponse() : String            │
│ + saveMessage() : void              │
│ + getConversationHistory() : List   │
└────────────────────────────────────┘
```

---

## 4.3 Input and Output Design

### **Input Design**

**System Inputs:**

1. **User Registration**
   - Email, Password, First Name, Last Name
   - Input Validation: Email format, Password strength (min 8 chars, uppercase, number, special char)

2. **User Login**
   - Email/Username, Password
   - Input Validation: Non-empty fields, valid credentials

3. **URL Analysis**
   - URL String
   - Input Validation: Valid URL format, non-empty

4. **Email Analysis**
   - Email Headers (Full Headers), Email Body (optional)
   - Input Validation: Proper email header format

5. **File Upload**
   - File (PDF, DOC, EXE, ZIP, Images)
   - File Size Limit: 50 MB
   - Input Validation: File type, size, virus scan ready

6. **Job Description Analysis**
   - Job Title, Job Description Text
   - Input Validation: Non-empty, minimum 50 characters

7. **Admin Commands**
   - User ID, Action Type, Parameters
   - Input Validation: Admin authorization, valid user ID

### **Output Design**

**System Outputs:**

1. **Analysis Results**
   - Risk Level (LOW, MEDIUM, HIGH) with color coding
   - Risk Score (0-100) with visual bar
   - Detailed Findings & Explanations
   - Actionable Recommendations

2. **Search Results**
   - Filtered Results List
   - Pagination (10-50 items per page)
   - Sort Options (Date, Risk Level, Type)

3. **User Dashboard**
   - Total Scans Count
   - High-Risk Scans Summary
   - Threat Distribution Chart
   - Recent Scan History

4. **Reports & Exports**
   - PDF Reports with comprehensive details
   - CSV/Excel exports of scan history
   - JSON format for API integration

5. **Admin Dashboard**
   - Active Users Count
   - System Health Status
   - Error/Alert Logs
   - Performance Metrics

6. **AI Chat Responses**
   - Formatted Text Responses
   - Markdown-supported formatting
   - Related Links & Resources

---

## 4.4 Algorithmic Design

### **Algorithm 1: URL Analysis Algorithm**

```
Input: URL string
Output: AnalysisResult with risk score and threats

Process:
1. Validate URL format
   IF URL format invalid THEN
      Return error message
   END IF

2. Extract domain and check reputation
   domain = extract_domain(URL)
   reputation = query_threat_database(domain)
   
3. Validate SSL certificate
   IF SSL valid THEN
      ssl_score = +20
   ELSE
      ssl_score = -30
   END IF

4. Check for URL patterns (shortened URLs, obfuscation, IP masking)
   IF contains_suspicious_patterns THEN
      pattern_score = -25
   ELSE
      pattern_score = 0
   END IF

5. Analyze redirect chains
   redirects = trace_redirects(URL)
   IF redirects > 3 THEN
      redirect_score = -20
   ELSE
      redirect_score = 0
   END IF

6. Calculate final risk score
   risk_score = reputation_score + ssl_score + pattern_score + redirect_score
   risk_score = clamp(risk_score, 0, 100)

7. Determine risk level
   IF risk_score < 30 THEN
      risk_level = "LOW"
   ELSE IF risk_score < 70 THEN
      risk_level = "MEDIUM"
   ELSE
      risk_level = "HIGH"
   END IF

8. Generate report with findings
   Create detailed report object with all analysis data
   Save to database
   Return AnalysisResult
```

### **Algorithm 2: Job Scam Detection Algorithm**

```
Input: job_title, job_description
Output: AnalysisResult with fraud risk score

Process:
1. Send job description to Gemini AI
   ai_response = call_gemini_api(job_description)
   ai_analysis = parse_gemini_response(ai_response)

2. Extract red flags
   red_flags = []
   
   IF contains("unrealistic salary") THEN
      red_flags.add("Unrealistic Salary Offer")
      flag_score = -20
   END IF
   
   IF contains("upfront payment") OR contains("fee required") THEN
      red_flags.add("Upfront Payment Required")
      flag_score = -25
   END IF
   
   IF contains("work from home") AND high_salary THEN
      red_flags.add("Too Good To Be True")
      flag_score = -15
   END IF
   
   IF contains("limited information") OR vague_role THEN
      red_flags.add("Vague Job Description")
      flag_score = -10
   END IF
   
   IF contains("urgent hiring") OR high_pressure THEN
      red_flags.add("Pressure Tactics")
      flag_score = -15
   END IF

3. Analyze company legitimacy
   company_name = extract_company_name(job_title)
   company_info = verify_company_existence(company_name)
   
   IF company verified THEN
      company_score = +20
   ELSE
      company_score = -20
   END IF

4. Calculate fraud risk score
   total_red_flags = count(red_flags)
   fraud_score = ai_analysis.confidence_score
   final_score = fraud_score + company_score - (total_red_flags * 5)
   final_score = clamp(final_score, 0, 100)

5. Determine fraud risk level
   IF final_score < 30 THEN
      risk_level = "LOW" (Likely Legitimate)
   ELSE IF final_score < 70 THEN
      risk_level = "MEDIUM" (Some Concern)
   ELSE
      risk_level = "HIGH" (Likely Scam)
   END IF

6. Generate AI recommendation
   recommendation = ai_response.recommendation
   safety_tips = generate_safety_tips(red_flags)
   
7. Save analysis and return result
   Save to database with all findings
   Return AnalysisResult with fraud analysis
```

### **Algorithm 3: Scan History Filtering Algorithm**

```
Input: user_id, filters (scan_type, risk_level, date_range, search_term)
Output: Filtered list of Scan records

Process:
1. Initialize query
   query = db.select().from(scans)
   query = query.where(scans.user_id == user_id)

2. Apply scan type filter (if provided)
   IF filter.scan_type is not null THEN
      query = query.where(scans.scan_type == filter.scan_type)
   END IF

3. Apply risk level filter (if provided)
   IF filter.risk_level is not null THEN
      query = query.where(scans.risk_level == filter.risk_level)
   END IF

4. Apply date range filter (if provided)
   IF filter.start_date is not null THEN
      query = query.where(scans.created_at >= filter.start_date)
   END IF
   IF filter.end_date is not null THEN
      query = query.where(scans.created_at <= filter.end_date)
   END IF

5. Apply search filter (if provided)
   IF filter.search_term is not null THEN
      query = query.where(
         scans.url LIKE search_term OR
         scans.email_from LIKE search_term OR
         scans.file_name LIKE search_term OR
         scans.job_title LIKE search_term
      )
   END IF

6. Apply sorting
   query = query.orderBy(scans.created_at DESC)

7. Apply pagination
   results = query.limit(page_size).offset(page_number * page_size)

8. Return filtered results
   Return paginated results
```

### **Algorithm 4: Risk Score Calculation (Generic)**

```
Input: Multiple analysis components with individual scores
Output: Consolidated risk score (0-100)

Process:
1. Initialize weighted scores
   threat_weight = 0.40
   reputation_weight = 0.30
   technical_weight = 0.20
   behavioral_weight = 0.10

2. Gather component scores
   threat_score = analyze_threat_indicators()
   reputation_score = check_reputation()
   technical_score = analyze_technical_factors()
   behavioral_score = analyze_behavioral_patterns()

3. Apply weights
   weighted_threat = threat_score * threat_weight
   weighted_reputation = reputation_score * reputation_weight
   weighted_technical = technical_score * technical_weight
   weighted_behavioral = behavioral_score * behavioral_weight

4. Calculate final score
   final_score = (
      weighted_threat +
      weighted_reputation +
      weighted_technical +
      weighted_behavioral
   )
   final_score = clamp(final_score, 0, 100)

5. Round to nearest 0.5
   final_score = round(final_score * 2) / 2

6. Assign risk level
   IF final_score <= 30 THEN
      risk_level = "LOW"
      color = "GREEN"
   ELSE IF final_score <= 70 THEN
      risk_level = "MEDIUM"
      color = "YELLOW"
   ELSE
      risk_level = "HIGH"
      color = "RED"
   END IF

7. Return result with score and level
```

---

## 4.5 API Design (REST Endpoints)

### **Core Endpoints**

```
Authentication:
POST   /api/v1/auth/register          - User registration
POST   /api/v1/auth/login             - User login
POST   /api/v1/auth/refresh-token     - Refresh JWT token
POST   /api/v1/auth/logout            - User logout

Analysis:
POST   /api/v1/analysis/url           - Analyze URL
POST   /api/v1/analysis/email         - Analyze email
POST   /api/v1/analysis/file          - Upload and scan file
POST   /api/v1/analysis/job           - Analyze job description

History:
GET    /api/v1/history                - Get user's scan history
GET    /api/v1/history/{id}           - Get specific scan details
GET    /api/v1/history/filter         - Filter scans (type/risk/date)
DELETE /api/v1/history/{id}           - Delete a scan
GET    /api/v1/history/statistics     - Get usage statistics
GET    /api/v1/history/high-risk      - Get high-risk scans summary
POST   /api/v1/history/export         - Export history as PDF/CSV

Chat:
POST   /api/v1/chat                   - Send message to AI
GET    /api/v1/chat/history/{id}      - Get conversation history

Admin:
GET    /api/v1/admin/users            - List all users
GET    /api/v1/admin/users/{id}       - Get user details
POST   /api/v1/admin/users/{id}/verify - Verify user
POST   /api/v1/admin/users/{id}/suspend - Suspend user
GET    /api/v1/admin/analytics        - Get system analytics
GET    /api/v1/admin/logs             - Get audit logs
GET    /api/v1/admin/health           - System health status
```

---

## Summary

**Chapter 4: System Design** provides:

1. **System Architecture** - Multi-tier architecture (Presentation, Application, Data)
2. **Physical Design** - Component, ER, and Class diagrams
3. **Input/Output Design** - Data formats and validation rules
4. **Algorithms** - 4 core algorithms for URL analysis, job detection, filtering, and scoring
5. **API Design** - RESTful endpoints for all system features

**Key Technologies**:
- Frontend: React.js + Material-UI
- Backend: Spring Boot + Java 17+
- Database: PostgreSQL + Redis Cache
- Storage: AWS S3
- External APIs: Google Gemini, OAuth2, Threat Detection APIs

---

**Document Version**: 1.0  
**Last Updated**: May 2026  
**Status**: Complete
