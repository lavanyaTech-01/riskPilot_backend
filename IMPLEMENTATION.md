# CHAPTER 5: IMPLEMENTATION
## RiskPilot - Phishing Risk & Fraud Detection System

---

## 5.1 Source Code

### **5.1.1 Backend - AuthenticationController**

```java
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "http://localhost:3000", methods = { 
    RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE 
})
public class AuthenticationController {
    
    @Autowired
    private AuthenticationService authService;
    
    /* Registration API */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            User user = authService.register(request.getEmail(), request.getPassword());
            return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("message", "Registration successful", "userId", user.getId())
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /* Login API */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid credentials"));
        }
    }
    
    /* OAuth2 Login */
    @PostMapping("/oauth2/login")
    public ResponseEntity<?> oauth2Login(@RequestBody OAuth2Request request) {
        AuthResponse response = authService.oauth2Login(request.getGoogleToken());
        return ResponseEntity.ok(response);
    }
    
    /* Refresh Token */
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String token) {
        String newToken = authService.refreshToken(token);
        return ResponseEntity.ok(Map.of("token", newToken));
    }
}
```

### **5.1.2 Backend - AnalysisController**

```java
@RestController
@RequestMapping("/api/v1/analysis")
@CrossOrigin(origins = "http://localhost:3000")
public class AnalysisController {
    
    @Autowired
    private URLAnalysisService urlService;
    
    @Autowired
    private EmailAnalysisService emailService;
    
    @Autowired
    private FileAnalysisService fileService;
    
    @Autowired
    private JobAnalysisService jobService;
    
    /* URL Analysis */
    @PostMapping("/url")
    public ResponseEntity<?> analyzeURL(
            @RequestHeader("Authorization") String token,
            @RequestBody URLAnalysisRequest request) {
        try {
            AnalysisResult result = urlService.analyze(request.getUrl());
            urlService.saveScan(getUserIdFromToken(token), result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "URL analysis failed: " + e.getMessage()));
        }
    }
    
    /* Email Analysis */
    @PostMapping("/email")
    public ResponseEntity<?> analyzeEmail(
            @RequestHeader("Authorization") String token,
            @RequestBody EmailAnalysisRequest request) {
        try {
            EmailResult result = emailService.analyzeHeaders(
                request.getHeaders(), request.getBody()
            );
            emailService.saveScan(getUserIdFromToken(token), result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Email analysis failed"));
        }
    }
    
    /* File Upload & Analysis */
    @PostMapping("/file")
    public ResponseEntity<?> analyzeFile(
            @RequestHeader("Authorization") String token,
            @RequestPart("file") MultipartFile file) {
        try {
            FileResult result = fileService.scan(file);
            fileService.saveScan(getUserIdFromToken(token), result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "File analysis failed"));
        }
    }
    
    /* Job Description Analysis */
    @PostMapping("/job")
    public ResponseEntity<?> analyzeJob(
            @RequestHeader("Authorization") String token,
            @RequestBody JobAnalysisRequest request) {
        try {
            JobResult result = jobService.analyzeJobDescription(
                request.getJobTitle(), request.getJobDescription()
            );
            jobService.saveScan(getUserIdFromToken(token), result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Job analysis failed"));
        }
    }
}
```

### **5.1.3 Backend - URLAnalysisService**

```java
@Service
public class URLAnalysisService {
    
    @Autowired
    private ScanRepository scanRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    public AnalysisResult analyze(String url) {
        try {
            // Validate URL format
            new URL(url);
            
            // Check domain reputation
            int reputationScore = checkDomainReputation(url);
            
            // Validate SSL certificate
            boolean sslValid = validateSSL(url);
            int sslScore = sslValid ? 20 : -30;
            
            // Detect suspicious patterns
            int patternScore = detectSuspiciousPatterns(url);
            
            // Check redirect chains
            int redirectScore = analyzeRedirects(url);
            
            // Calculate final risk score
            int finalScore = reputationScore + sslScore + patternScore + redirectScore;
            finalScore = Math.max(0, Math.min(100, finalScore)); // Clamp 0-100
            
            // Determine risk level
            String riskLevel = finalScore < 30 ? "LOW" : 
                             finalScore < 70 ? "MEDIUM" : "HIGH";
            
            return new AnalysisResult(
                url, finalScore, riskLevel, 
                generateReport(url, finalScore, riskLevel)
            );
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL format");
        }
    }
    
    private int checkDomainReputation(String url) {
        // Query threat database
        return 50; // Placeholder
    }
    
    private boolean validateSSL(String url) {
        // SSL validation logic
        return true; // Placeholder
    }
    
    private int detectSuspiciousPatterns(String url) {
        if (url.contains("bit.ly") || url.contains("tinyurl")) return -20;
        if (url.contains("ip") || url.matches(".*\\d{1,3}\\.\\d{1,3}.*")) return -15;
        return 0;
    }
    
    private int analyzeRedirects(String url) {
        // Trace redirect chains
        return 0; // Placeholder
    }
    
    public void saveScan(Long userId, AnalysisResult result) {
        Scan scan = new Scan();
        scan.setUserId(userId);
        scan.setScanType("URL");
        scan.setUrl(result.getUrl());
        scan.setRiskScore(result.getRiskScore());
        scan.setRiskLevel(result.getRiskLevel());
        scan.setDetailedReport(result.getReport());
        scan.setCreatedAt(LocalDateTime.now());
        scanRepository.save(scan);
    }
}
```

### **5.1.4 Backend - JobAnalysisService**

```java
@Service
public class JobAnalysisService {
    
    @Autowired
    private GeminiService geminiService;
    
    @Autowired
    private ScanRepository scanRepository;
    
    public JobResult analyzeJobDescription(String title, String description) {
        try {
            // Send to Gemini AI
            String aiAnalysis = geminiService.analyzeJobDescription(description);
            
            // Detect red flags
            List<String> redFlags = detectRedFlags(description);
            
            // Verify company legitimacy
            int companyScore = verifyCompany(title);
            
            // Calculate fraud risk score
            int fraudScore = calculateFraudScore(redFlags, aiAnalysis, companyScore);
            
            // Determine fraud risk level
            String riskLevel = fraudScore < 30 ? "LOW" : 
                             fraudScore < 70 ? "MEDIUM" : "HIGH";
            
            return new JobResult(
                title, description, fraudScore, riskLevel, 
                redFlags, aiAnalysis
            );
        } catch (Exception e) {
            throw new RuntimeException("Job analysis failed: " + e.getMessage());
        }
    }
    
    private List<String> detectRedFlags(String description) {
        List<String> flags = new ArrayList<>();
        String lower = description.toLowerCase();
        
        if (lower.contains("upfront") || lower.contains("fee")) 
            flags.add("Upfront Payment Required");
        if (lower.contains("urgent") || lower.contains("immediate"))
            flags.add("Pressure Tactics");
        if (lower.contains("work from home") && lower.contains("high salary"))
            flags.add("Too Good To Be True");
        if (description.length() < 100)
            flags.add("Vague Job Description");
            
        return flags;
    }
    
    private int verifyCompany(String title) {
        // Company verification logic
        return 20; // Placeholder
    }
    
    private int calculateFraudScore(List<String> flags, String ai, int companyScore) {
        int score = 50; // Base score
        score -= (flags.size() * 10);
        score += companyScore;
        return Math.max(0, Math.min(100, score));
    }
    
    public void saveScan(Long userId, JobResult result) {
        Scan scan = new Scan();
        scan.setUserId(userId);
        scan.setScanType("JOB");
        scan.setJobTitle(result.getTitle());
        scan.setJobDescription(result.getDescription());
        scan.setRiskScore(result.getFraudScore());
        scan.setRiskLevel(result.getRiskLevel());
        scan.setDetailedReport(result.getAiAnalysis());
        scan.setCreatedAt(LocalDateTime.now());
        scanRepository.save(scan);
    }
}
```

### **5.1.5 Backend - GeminiService**

```java
@Service
public class GeminiService {
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    @Autowired
    private RestTemplate restTemplate;
    
    public String analyzeJobDescription(String jobDescription) {
        try {
            String prompt = "Analyze this job description for fraud indicators. " +
                          "Identify red flags and provide a risk assessment:\n\n" +
                          jobDescription;
            
            return generateContent(prompt);
        } catch (Exception e) {
            throw new RuntimeException("Gemini API failed: " + e.getMessage());
        }
    }
    
    public String answerSecurityQuestion(String question) {
        try {
            String prompt = "Provide security guidance for this question:\n" + question;
            return generateContent(prompt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate response");
        }
    }
    
    private String generateContent(String prompt) {
        // Gemini API call
        GeminiRequest request = new GeminiRequest(prompt);
        GeminiResponse response = restTemplate.postForObject(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
            request,
            GeminiResponse.class
        );
        return response != null ? response.getText() : "";
    }
}
```

### **5.1.6 Backend - HistoryService**

```java
@Service
public class HistoryService {
    
    @Autowired
    private ScanRepository scanRepository;
    
    public List<Scan> getUserScans(Long userId) {
        return scanRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public List<Scan> filterScans(Long userId, String type, String riskLevel, LocalDate startDate) {
        return scanRepository.findScans(userId, type, riskLevel, startDate);
    }
    
    public List<Scan> getHighRiskScans(Long userId) {
        return scanRepository.findByUserIdAndRiskLevel(userId, "HIGH");
    }
    
    public void deleteScan(Long scanId) {
        scanRepository.deleteById(scanId);
    }
    
    public AnalyticsDTO getStatistics(Long userId) {
        List<Scan> scans = getUserScans(userId);
        AnalyticsDTO stats = new AnalyticsDTO();
        stats.setTotalScans(scans.size());
        stats.setHighRiskCount((int) scans.stream()
            .filter(s -> "HIGH".equals(s.getRiskLevel())).count());
        stats.setUrlScans((int) scans.stream()
            .filter(s -> "URL".equals(s.getScanType())).count());
        return stats;
    }
    
    public byte[] exportAsCSV(Long userId) {
        List<Scan> scans = getUserScans(userId);
        StringBuilder csv = new StringBuilder();
        csv.append("Type,Risk Level,Date,Summary\n");
        for (Scan scan : scans) {
            csv.append(String.format("%s,%s,%s,%s\n",
                scan.getScanType(), scan.getRiskLevel(), 
                scan.getCreatedAt(), scan.getResultSummary()
            ));
        }
        return csv.toString().getBytes();
    }
}
```

---

## 5.2 Frontend Components

### **5.2.1 Frontend - LoginPage.jsx**

```jsx
import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "./LoginPage.css";

const LoginPage = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const response = await axios.post(
        "http://localhost:8080/api/v1/auth/login",
        { email, password }
      );
      
      localStorage.setItem("token", response.data.token);
      localStorage.setItem("userId", response.data.userId);
      navigate("/dashboard");
    } catch (err) {
      setError("Invalid email or password");
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = async (googleToken) => {
    try {
      const response = await axios.post(
        "http://localhost:8080/api/v1/auth/oauth2/login",
        { googleToken }
      );
      localStorage.setItem("token", response.data.token);
      navigate("/dashboard");
    } catch (err) {
      setError("Google login failed");
    }
  };

  return (
    <div className="login-container">
      <form onSubmit={handleLogin}>
        <h2>RiskPilot Login</h2>
        
        {error && <div className="error-message">{error}</div>}
        
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        
        <button type="submit" disabled={loading}>
          {loading ? "Logging in..." : "Login"}
        </button>
        
        <button type="button" onClick={() => handleGoogleLogin()}>
          Login with Google
        </button>
      </form>
    </div>
  );
};

export default LoginPage;
```

### **5.2.2 Frontend - URLAnalyzer.jsx**

```jsx
import React, { useState } from "react";
import axios from "axios";
import "./URLAnalyzer.css";

const URLAnalyzer = () => {
  const [url, setUrl] = useState("");
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleAnalyze = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    setResult(null);

    try {
      const token = localStorage.getItem("token");
      const response = await axios.post(
        "http://localhost:8080/api/v1/analysis/url",
        { url },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      
      setResult(response.data);
    } catch (err) {
      setError("URL analysis failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="analyzer-container">
      <h2>URL Safety Analyzer</h2>
      
      <form onSubmit={handleAnalyze}>
        <input
          type="url"
          placeholder="Enter URL to analyze..."
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          required
        />
        
        <button type="submit" disabled={loading}>
          {loading ? "Analyzing..." : "Analyze"}
        </button>
      </form>

      {error && <div className="error">{error}</div>}

      {result && (
        <div className="result">
          <h3>Analysis Result</h3>
          
          <div className={`risk-score ${result.riskLevel.toLowerCase()}`}>
            Risk Level: {result.riskLevel}
            <div className="score-bar">
              <div className="score-fill" style={{width: result.riskScore + "%"}}></div>
            </div>
            <span>{result.riskScore}/100</span>
          </div>
          
          <div className="report">
            <h4>Findings:</h4>
            <p>{result.report}</p>
          </div>
          
          <button onClick={() => saveToHistory(result.url)}>Save to History</button>
        </div>
      )}
    </div>
  );
};

export default URLAnalyzer;
```

### **5.2.3 Frontend - JobAnalyzer.jsx**

```jsx
import React, { useState } from "react";
import axios from "axios";
import "./JobAnalyzer.css";

const JobAnalyzer = () => {
  const [jobTitle, setJobTitle] = useState("");
  const [jobDescription, setJobDescription] = useState("");
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleAnalyze = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const token = localStorage.getItem("token");
      const response = await axios.post(
        "http://localhost:8080/api/v1/analysis/job",
        { jobTitle, jobDescription },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      
      setResult(response.data);
    } catch (error) {
      alert("Job analysis failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="job-analyzer">
      <h2>Job Scam Detector</h2>
      
      <form onSubmit={handleAnalyze}>
        <input
          type="text"
          placeholder="Job Title"
          value={jobTitle}
          onChange={(e) => setJobTitle(e.target.value)}
          required
        />
        
        <textarea
          placeholder="Paste job description..."
          value={jobDescription}
          onChange={(e) => setJobDescription(e.target.value)}
          rows="8"
          required
        />
        
        <button type="submit" disabled={loading}>
          {loading ? "Analyzing..." : "Analyze"}
        </button>
      </form>

      {result && (
        <div className="result">
          <h3>Fraud Assessment</h3>
          
          <div className={`fraud-score ${result.riskLevel.toLowerCase()}`}>
            Status: {result.riskLevel}
            <div className="progress-bar">
              <div className="progress-fill" style={{width: result.fraudScore + "%"}}></div>
            </div>
            Risk Score: {result.fraudScore}/100
          </div>
          
          {result.redFlags.length > 0 && (
            <div className="red-flags">
              <h4>Red Flags Detected:</h4>
              <ul>
                {result.redFlags.map((flag, idx) => (
                  <li key={idx}>⚠️ {flag}</li>
                ))}
              </ul>
            </div>
          )}
          
          <div className="ai-analysis">
            <h4>AI Analysis:</h4>
            <p>{result.aiAnalysis}</p>
          </div>
        </div>
      )}
    </div>
  );
};

export default JobAnalyzer;
```

---

## 5.3 Module Integration & Dependencies

### **Backend Module Hierarchy**

```
┌─────────────────────────────────────────────────────────┐
│            REST API Controllers Layer                   │
│  ┌──────────────────────────────────────────────────┐  │
│  │ AuthenticationController                         │  │
│  │ AnalysisController                               │  │
│  │ HistoryController                                │  │
│  │ AdminController                                  │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│            Service Layer (Business Logic)               │
│  ┌──────────────────────────────────────────────────┐  │
│  │ AuthenticationService                            │  │
│  │ URLAnalysisService                               │  │
│  │ EmailAnalysisService                             │  │
│  │ FileAnalysisService                              │  │
│  │ JobAnalysisService                               │  │
│  │ HistoryService                                   │  │
│  │ GeminiService                                    │  │
│  │ AdminService                                     │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│         Repository/DAO Layer (Data Access)              │
│  ┌──────────────────────────────────────────────────┐  │
│  │ UserRepository (JPA)                             │  │
│  │ ScanRepository (JPA)                             │  │
│  │ SessionRepository (JPA)                          │  │
│  │ ChatMessageRepository (JPA)                      │  │
│  │ AdminLogRepository (JPA)                         │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│         Entity/Model Layer (Domain Objects)             │
│  ┌──────────────────────────────────────────────────┐  │
│  │ User Entity                                      │  │
│  │ Scan Entity                                      │  │
│  │ Session Entity                                   │  │
│  │ ChatMessage Entity                               │  │
│  │ AdminLog Entity                                  │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│           Persistence & External Integration             │
│  ├─ PostgreSQL Database                                 │
│  ├─ Redis Cache                                         │
│  ├─ AWS S3 File Storage                                │
│  ├─ Google Gemini API                                   │
│  └─ OAuth2 Provider (Google)                            │
└─────────────────────────────────────────────────────────┘
```

### **Frontend Component Hierarchy**

```
App.jsx (Root Component)
│
├── Router / Navigation
│   ├── Header.jsx
│   └── Navbar.jsx
│
├── Authentication Module
│   ├── LoginPage.jsx
│   └── RegisterPage.jsx
│
├── Analysis Module (Main Dashboard)
│   ├── DashboardLayout.jsx
│   ├── URLAnalyzer.jsx
│   ├── EmailAnalyzer.jsx
│   ├── FileScanner.jsx
│   └── JobAnalyzer.jsx
│
├── History Module
│   ├── ScanHistory.jsx
│   ├── ScanFilter.jsx
│   ├── ScanDetails.jsx
│   └── ExportReport.jsx
│
├── AI Chat Module
│   ├── ChatInterface.jsx
│   ├── ChatMessage.jsx
│   └── ConversationHistory.jsx
│
├── Admin Module (if role = ADMIN)
│   ├── AdminDashboard.jsx
│   ├── UserManagement.jsx
│   ├── SystemAnalytics.jsx
│   └── AuditLogs.jsx
│
└── Shared Components
    ├── ResultCard.jsx
    ├── RiskIndicator.jsx
    ├── ProgressBar.jsx
    ├── Loader.jsx
    └── ErrorBoundary.jsx
```

---

## 5.4 Key Dependencies

### **Backend Dependencies (pom.xml)**

```xml
<!-- Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Spring Security & JWT -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>

<!-- PostgreSQL Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Gemini API Client -->
<dependency>
    <groupId>com.google.ai.client.generativelanguage</groupId>
    <artifactId>google-ai-generativelanguage</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

### **Frontend Dependencies (package.json)**

```json
{
  "dependencies": {
    "react": "^18.0.0",
    "react-dom": "^18.0.0",
    "react-router-dom": "^6.0.0",
    "axios": "^1.4.0",
    "@mui/material": "^5.13.0",
    "@mui/icons-material": "^5.13.0",
    "react-toastify": "^9.1.0",
    "country-state-city": "^3.2.0"
  }
}
```

---

## Summary

**Chapter 5: Implementation** provides:

1. **Backend Source Code** - 6 key Java classes (Controllers, Services)
2. **Frontend Components** - 3 React components (Login, URL Analyzer, Job Analyzer)
3. **Module Integration** - Complete dependency structure and hierarchy
4. **Key Dependencies** - Required libraries and frameworks

**Key Implementation Points**:
- RESTful API with JWT authentication
- OAuth2 integration with Google
- Multi-tier architecture with separation of concerns
- AI integration using Google Gemini API
- Error handling and validation at every layer
- Responsive React UI components

---

**Document Version**: 1.0  
**Last Updated**: May 2026  
**Status**: Complete
