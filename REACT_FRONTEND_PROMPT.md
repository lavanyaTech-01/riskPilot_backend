# RiskPilot - React Frontend Implementation Prompt

## Project Overview
RiskPilot is an AI-powered security analysis platform that helps users identify scams, phishing attempts, and fraudulent job offers. The platform uses Google Gemini AI to analyze documents, URLs, and email addresses for security risks.

---

## Technology Stack Requirements

### Core Libraries
- **React 18+** - UI Framework
- **React Router v6+** - Navigation and routing
- **Axios** - HTTP client for API communication
- **Redux Toolkit** - State management
- **TypeScript** - Type safety
- **Tailwind CSS** - Styling framework
- **React Query** - Server state management (optional but recommended)

### UI Components & Utilities
- **React Hot Toast** - Toast notifications
- **React Icons** - Icon library
- **Recharts** - Data visualization for analytics
- **Date-fns** - Date formatting utilities
- **React Helmet** - SEO management
- **Zod** - Schema validation

### Authentication & Security
- **JWT Token** - Token-based authentication
- **Google OAuth 2.0** - OAuth login integration
- **@react-oauth/google** - Google OAuth library

### Development Tools
- **Vite** - Build tool (optional, recommended for faster builds)
- **ESLint** - Code linting
- **Prettier** - Code formatting

---

## Project Structure

```
src/
├── components/
│   ├── Auth/
│   │   ├── LoginPage.tsx
│   │   ├── SignupPage.tsx
│   │   ├── OAuth2LoginButton.tsx
│   │   └── GoogleLoginForm.tsx
│   ├── Dashboard/
│   │   ├── Dashboard.tsx
│   │   ├── DashboardStats.tsx
│   │   └── RecentScans.tsx
│   ├── Scan/
│   │   ├── FileUploader.tsx
│   │   ├── UrlAnalyzer.tsx
│   │   ├── EmailAnalyzer.tsx
│   │   ├── DescriptionAnalyzer.tsx
│   │   └── ScanResults.tsx
│   ├── History/
│   │   ├── HistoryPage.tsx
│   │   ├── ScanFilterPanel.tsx
│   │   ├── ScanTable.tsx
│   │   └── ScanDetailModal.tsx
│   ├── Layout/
│   │   ├── Header.tsx
│   │   ├── Sidebar.tsx
│   │   ├── Footer.tsx
│   │   └── MainLayout.tsx
│   ├── Common/
│   │   ├── RiskBadge.tsx
│   │   ├── TrustScoreDisplay.tsx
│   │   ├── LoadingSpinner.tsx
│   │   └── ErrorBoundary.tsx
│   └── Profile/
│       ├── ProfilePage.tsx
│       └── UserSettings.tsx
├── services/
│   ├── api/
│   │   ├── auth.ts
│   │   ├── scan.ts
│   │   ├── history.ts
│   │   └── client.ts (Axios instance)
│   └── utils/
│       ├── formatters.ts
│       ├── validators.ts
│       └── constants.ts
├── store/
│   ├── authSlice.ts
│   ├── scanSlice.ts
│   ├── historySlice.ts
│   └── store.ts
├── hooks/
│   ├── useAuth.ts
│   ├── useLocalStorage.ts
│   └── useApi.ts
├── types/
│   ├── api.ts
│   ├── models.ts
│   └── auth.ts
├── styles/
│   ├── globals.css
│   └── variables.css
├── App.tsx
├── App.css
└── main.tsx
```

---

## API Integration Requirements

### Base URL Configuration
- Backend API Base URL: `http://localhost:8080/api` (configurable via environment variables)
- All API requests should include JWT token in Authorization header: `Bearer {token}`
- CORS should be handled from backend

### Authentication Endpoints

#### 1. User Registration
```
POST /auth/signup
Request:
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "securePassword123"
}

Response (Success - 201):
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "createdAt": "2024-03-30T10:00:00"
}

Response (Error - 400):
{
  "message": "Email already exists"
}
```

#### 2. User Login (Email/Password)
```
POST /auth/login
Request:
{
  "email": "john@example.com",
  "password": "securePassword123"
}

Response (Success - 200):
{
  "message": "Login successful!",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com"
  }
}
```

#### 3. Google OAuth Login/Signup
```
POST /auth/signup or /auth/login
Request:
{
  "googleToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEifQ..."
}

Response (Success - 200/201):
{
  "message": "OAuth Login successful!",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "email": "john@gmail.com",
    "name": "John Doe"
  }
}
```

### Scan Endpoints

#### 1. File Scan (PDF, Image, Text)
```
POST /scan/file
Content-Type: multipart/form-data
Headers: Authorization: Bearer {token}

Request:
- file: [File] (PDF, PNG, JPG, TXT, CSV, etc.)

Response (Success - 200):
{
  "riskLevel": "HIGH|MEDIUM|LOW",
  "trustScore": 2.5,
  "email": "recruiter@suspicious.com",
  "url": "https://suspicious-link.com",
  "emailVerified": false,
  "companyVerified": false,
  "companyDetails": "Unknown company",
  "suspiciousIndicators": [
    "Urgency language detected",
    "Request for upfront payment",
    "Vague job description"
  ],
  "suggestions": [
    "Verify the company's official website",
    "Check employment history",
    "Never send personal information"
  ],
  "analysisSummary": "This appears to be a potential scam with multiple red flags...",
  "reviewAnalysis": {
    "companyName": "Company Name",
    "averageRating": 3.5,
    "totalReviews": 150,
    "scamReports": 5,
    "positiveReviews": 120,
    "negativeReviews": 30
  },
  "reviewBasedRiskAdjustment": "Risk level adjusted due to negative reviews"
}

Response (Error - 400):
{
  "message": "File is required",
  "errorCode": "file_missing"
}
```

#### 2. URL Analysis
```
POST /scan/url
Headers: Authorization: Bearer {token}
Content-Type: application/json

Request:
{
  "url": "https://suspicious-job-link.com",
  "knownCompanyDomain": "amazon.com",
  "senderEmail": "recruiter@suspicious.com",
  "companyName": "Amazon Careers"
}

Response (Success - 200):
{
  "riskLevel": "HIGH|MEDIUM|LOW",
  "trustScore": 2.5,
  "domainAgeDays": 12,
  "domainCreationDate": "2024-03-15",
  "domainRegistrar": "GoDaddy",
  "sslValid": true,
  "redirectDetected": false,
  "malwareFlagged": true,
  "dnsResolved": true,
  "typosquattingDetected": true,
  "suspiciousIndicators": [
    "Domain registered recently",
    "Malware detected on domain",
    "Typosquatting pattern detected"
  ],
  "suggestions": [
    "Avoid clicking links from this URL",
    "Verify the original company website",
    "Report to relevant authorities"
  ],
  "analysisSummary": "This URL exhibits multiple security concerns..."
}
```

#### 3. Email Analysis
```
POST /scan/email
Headers: Authorization: Bearer {token}
Content-Type: application/json

Request:
{
  "email": "hr@xyz-careers-job.com"
}

Response (Success - 200):
{
  "riskLevel": "HIGH|MEDIUM|LOW",
  "trustScore": 15,
  "riskScore": 85,
  "email": "hr@xyz-careers-job.com",
  "emailDomain": "xyz-careers-job.com",
  "isFreeEmailProvider": false,
  "domainType": "SUSPICIOUS|UNKNOWN|TRUSTED",
  "isLookalikeDomain": true,
  "phishingSimilarityScore": 82,
  "domainSuspiciousIndicators": ["jobs", "careers"],
  "suspiciousIndicators": [
    "Email uses custom domain with no verified reputation",
    "Phishing pattern detected (score: 82%)",
    "Domain pattern resembles phishing/scam attempts"
  ],
  "suggestions": [
    "Verify through official company channels",
    "Check if domain is registered to the company",
    "Be cautious with this sender"
  ],
  "analysisSummary": "This email address shows signs of phishing..."
}
```

#### 4. Job Description Analysis
```
POST /scan/description
Headers: Authorization: Bearer {token}
Content-Type: application/json

Request:
{
  "description": "Job description text here..."
}

Response (Success - 200):
{
  "riskLevel": "HIGH|MEDIUM|LOW",
  "trustScore": 4,
  "suspiciousIndicators": [
    "Urgency language detected",
    "Vague responsibilities listed"
  ],
  "suggestions": [
    "Request detailed job description",
    "Ask for company contact information"
  ],
  "analysisSummary": "..."
}
```

### History Endpoints

#### 1. Get All History
```
GET /history/all
Headers: Authorization: Bearer {token}

Response (Success - 200):
{
  "success": true,
  "totalRecords": 25,
  "data": [
    {
      "id": 1,
      "scanType": "FILE|URL|EMAIL|DESCRIPTION",
      "scanContent": "filename.pdf or url or email",
      "riskLevel": "HIGH|MEDIUM|LOW",
      "trustScore": 2.5,
      "timestamp": "2024-03-30T10:30:00",
      "response": "{...serialized response...}"
    },
    ...
  ]
}
```

#### 2. Get Scan Details by ID
```
GET /history/{scanId}
Headers: Authorization: Bearer {token}

Response (Success - 200):
{
  "success": true,
  "data": {
    "id": 1,
    "scanType": "FILE",
    "scanContent": "file.pdf",
    "riskLevel": "HIGH",
    "trustScore": 2.5,
    "timestamp": "2024-03-30T10:30:00",
    "aiAnalysisResponse": {
      "riskLevel": "HIGH",
      "trustScore": 2.5,
      ...
    }
  }
}
```

#### 3. Filter History
```
GET /history/filter?riskLevel=HIGH&startDate=2024-01-01&endDate=2024-12-31
Headers: Authorization: Bearer {token}

Query Parameters:
- riskLevel: HIGH | MEDIUM | LOW (optional)
- startDate: YYYY-MM-DD (optional)
- endDate: YYYY-MM-DD (optional)
- scanType: FILE | URL | EMAIL | DESCRIPTION (optional)

Response (Success - 200):
{
  "success": true,
  "totalRecords": 10,
  "data": [...]
}
```

#### 4. Delete Scan Record
```
DELETE /history/{scanId}
Headers: Authorization: Bearer {token}

Response (Success - 200):
{
  "success": true,
  "message": "Scan deleted successfully"
}
```

---

## Frontend Features to Implement

### 1. Authentication & Authorization
- [ ] User registration with email and password
- [ ] User login with email and password
- [ ] Google OAuth 2.0 integration (Sign in with Google)
- [ ] JWT token management (store in secure storage/cookie)
- [ ] Automatic token refresh on expiration
- [ ] Logout functionality with token cleanup
- [ ] Protected routes (redirect to login if not authenticated)
- [ ] Remember me functionality
- [ ] Password reset flow (if available in backend)

### 2. Dashboard
- [ ] Welcome message with user's name
- [ ] Summary statistics (total scans, high-risk reports, etc.)
- [ ] Recent scans widget (last 5 scans)
- [ ] Quick action buttons (Upload file, Check URL, Analyze Email)
- [ ] Risk level distribution chart (pie/bar chart)
- [ ] Responsive layout for mobile and desktop
- [ ] Dark mode support

### 3. File Upload & Analysis
- [ ] Drag-and-drop file upload interface
- [ ] File type validation (PDF, images, text files)
- [ ] File size validation with error message
- [ ] Upload progress indicator
- [ ] Loading state during analysis
- [ ] Display analysis results with risk assessment
- [ ] Show suspicious indicators in a list format
- [ ] Show suggestions in a highlighted box
- [ ] Display analysis summary with proper formatting
- [ ] Option to save results to history
- [ ] Option to download results as PDF

### 4. URL Analysis
- [ ] URL input field with validation
- [ ] Optional context fields:
  - Known company domain
  - Sender email address
  - Company name
- [ ] Display comprehensive URL analysis results
- [ ] Show domain age and registrar information
- [ ] SSL certificate validation status
- [ ] Redirect detection warning
- [ ] Malware flagged indicator
- [ ] DNS resolution status
- [ ] Typosquatting detection
- [ ] Risk level indicator with color coding

### 5. Email Analysis
- [ ] Email input field with validation
- [ ] Display email domain analysis
- [ ] Identify free email providers with warning
- [ ] Show phishing similarity score
- [ ] Domain lookalike detection
- [ ] Trust score on 0-100 scale
- [ ] Suspicious indicators highlighting
- [ ] Recommendations section

### 6. Job Description Analysis
- [ ] Text area for job description input
- [ ] Character count display
- [ ] Analysis of job posting for scam indicators
- [ ] Risk level assessment
- [ ] Suggestions for verification
- [ ] Analysis summary display

### 7. Results Display
- [ ] Risk level badge (HIGH/MEDIUM/LOW) with color coding:
  - HIGH = Red (#DC2626)
  - MEDIUM = Yellow/Orange (#F59E0B)
  - LOW = Green (#10B981)
- [ ] Trust score visualization (0-10 or 0-100 scale)
- [ ] Suspicious indicators list with icons
- [ ] Suggestions list with icons
- [ ] Analysis summary in readable format
- [ ] Review analysis if available (company rating, reviews count, scam reports)
- [ ] Copy to clipboard functionality for results
- [ ] Share results functionality
- [ ] Print results functionality

### 8. Scan History
- [ ] Display all user scans in a table/list format
- [ ] Columns: Scan ID, Type, Content, Risk Level, Date, Action buttons
- [ ] Sorting by date, risk level, scan type
- [ ] Pagination (10, 25, 50 items per page)
- [ ] Filter by risk level (High, Medium, Low)
- [ ] Filter by scan type (File, URL, Email, Description)
- [ ] Filter by date range (calendar picker)
- [ ] Search functionality (by filename, URL, email, etc.)
- [ ] View detailed scan results in a modal/side panel
- [ ] Delete scan record with confirmation dialog
- [ ] Bulk delete functionality
- [ ] Export scan history as CSV/Excel
- [ ] No-data state with helpful message

### 9. User Profile & Settings
- [ ] Display user information (name, email, registration date)
- [ ] Edit profile information
- [ ] Change password functionality
- [ ] View account security settings
- [ ] Two-factor authentication setup (if available)
- [ ] Notification preferences
- [ ] Privacy settings
- [ ] Account deletion with confirmation
- [ ] Activity log/login history
- [ ] Download user data

### 10. Navigation & Layout
- [ ] Responsive header with logo and navigation
- [ ] Sidebar navigation menu (collapsible on mobile)
- [ ] Active route highlighting
- [ ] User profile dropdown menu
- [ ] Logout button
- [ ] Breadcrumb navigation
- [ ] Footer with links and information
- [ ] Mobile hamburger menu
- [ ] Sticky header on scroll

### 11. Error Handling & Loading States
- [ ] Global error boundary component
- [ ] Loading spinners for async operations
- [ ] Skeleton loaders for data tables
- [ ] Toast notifications for success/error/warning messages
- [ ] Retry buttons for failed requests
- [ ] Timeout handling with user-friendly messages
- [ ] Network error detection and messaging
- [ ] API error response handling (400, 401, 403, 404, 500, 503, etc.)
- [ ] Form validation errors display
- [ ] Empty state messages

### 12. Analytics & Insights
- [ ] Risk level distribution chart (pie chart)
- [ ] Scans over time chart (line/area chart)
- [ ] Scan type distribution (bar chart)
- [ ] Summary statistics cards (total scans, avg risk score, etc.)
- [ ] Most common suspicious indicators
- [ ] Monthly/weekly scan trends

### 13. Accessibility & UX
- [ ] WCAG 2.1 AA compliance
- [ ] Keyboard navigation
- [ ] Screen reader support (semantic HTML)
- [ ] Focus indicators for keyboard users
- [ ] Color contrast ratios
- [ ] Responsive design (mobile-first approach)
- [ ] Touch-friendly button sizes (min 44x44px)
- [ ] Loading and disabled states for buttons
- [ ] Confirmation dialogs for destructive actions
- [ ] Tooltips and help text

### 14. Security Features
- [ ] Secure token storage (HttpOnly cookies or secure localStorage)
- [ ] HTTPS enforcement
- [ ] CSRF token handling
- [ ] XSS protection
- [ ] SQL injection prevention (API side, but frontend should validate)
- [ ] Sanitize user input
- [ ] Prevent sensitive data leaks in console logs
- [ ] Secure API communication
- [ ] Timeout for inactive sessions

### 15. Performance Optimization
- [ ] Code splitting and lazy loading for routes
- [ ] Image optimization and lazy loading
- [ ] Debouncing for search and filter operations
- [ ] Caching of API responses (React Query)
- [ ] Memoization for expensive computations
- [ ] Bundle size optimization
- [ ] CSS optimization with Tailwind purging
- [ ] Minimize re-renders

---

## Design & UI Requirements

### Color Scheme (Professional/Enterprise)
```
Primary: #0D47A1 (Deep Blue)
Secondary: #1565C0 (Medium Blue)
Success: #10B981 (Green)
Warning: #F59E0B (Orange)
Danger: #DC2626 (Red)
Neutral: #6B7280 (Gray)
Background: #F9FAFB (Light Gray)
Text: #111827 (Dark Gray)
```

### Risk Level Color Coding
```
HIGH RISK:    #DC2626 (Red)
MEDIUM RISK:  #F59E0B (Orange/Yellow)
LOW RISK:     #10B981 (Green)
```

### Typography
```
Headers: Inter, Segoe UI, or system font stack
Body: Inter, Segoe UI, or system font stack
Monospace: Fira Code for code snippets
Font Sizes: 12px, 14px, 16px, 18px, 20px, 24px, 32px, 48px
```

### Components Style
- Rounded corners: 6-8px for buttons, 8-12px for cards
- Box shadows: Subtle shadows for depth
- Padding/Margin: 8px unit grid system (8, 16, 24, 32, etc.)
- Hover states on interactive elements
- Smooth transitions (200-300ms)

---

## Environment Variables

Create `.env.local` file:
```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_GOOGLE_CLIENT_ID=your_google_client_id_here
VITE_APP_NAME=RiskPilot
VITE_APP_VERSION=1.0.0
VITE_ENABLE_ANALYTICS=true
```

---

## State Management (Redux Store)

### Auth Slice
```typescript
{
  user: {
    id: number | null,
    name: string | null,
    email: string | null,
    profilePicture: string | null
  },
  token: string | null,
  isAuthenticated: boolean,
  loading: boolean,
  error: string | null
}
```

### Scan Slice
```typescript
{
  currentResult: AiAnalysisResponseDto | null,
  loading: boolean,
  error: string | null,
  scanType: 'FILE' | 'URL' | 'EMAIL' | 'DESCRIPTION' | null,
  lastScannedContent: string | null
}
```

### History Slice
```typescript
{
  scans: HistoryReport[],
  currentScan: HistoryReport | null,
  loading: boolean,
  error: string | null,
  filters: {
    riskLevel: string | null,
    scanType: string | null,
    startDate: Date | null,
    endDate: Date | null,
    searchQuery: string
  },
  pagination: {
    page: number,
    pageSize: number,
    totalRecords: number
  }
}
```

---

## Testing Requirements

- [ ] Unit tests for utility functions (formatters, validators)
- [ ] Component testing for UI components
- [ ] Integration tests for API calls
- [ ] E2E tests for critical user flows
- [ ] Accessibility testing
- [ ] Performance testing (Lighthouse score > 80)

---

## Deployment Considerations

- [ ] Build optimization
- [ ] Environment-based configuration
- [ ] Error tracking (Sentry or similar)
- [ ] Analytics (Google Analytics or similar)
- [ ] CDN for static assets
- [ ] Compression (gzip)
- [ ] Security headers (CSP, HSTS, etc.)

---

## Notes for Frontend Team

1. **Error Handling**: Always show user-friendly error messages. Don't expose sensitive backend errors.

2. **Loading States**: Provide visual feedback for all async operations (uploads, API calls).

3. **Data Validation**: Validate all user input on the frontend before sending to the backend.

4. **Responsive Design**: Ensure the application works seamlessly on mobile, tablet, and desktop.

5. **Accessibility**: Follow WCAG guidelines for accessibility. Use semantic HTML and ARIA labels.

6. **Security**: Never log sensitive information (tokens, passwords, personal data).

7. **Performance**: Monitor bundle size and keep it under 500KB gzipped for optimal performance.

8. **User Feedback**: Provide clear feedback for user actions (success messages, error alerts, confirmations).

9. **Documentation**: Maintain comprehensive component documentation and API integration guide.

10. **Version Control**: Use meaningful commit messages and maintain a clean Git history.

---

## Frontend Folder Structure for Reference

```
riskpilot-frontend/
├── public/
│   ├── favicon.ico
│   ├── logo.png
│   └── index.html
├── src/
│   ├── components/
│   ├── pages/
│   ├── services/
│   ├── store/
│   ├── hooks/
│   ├── types/
│   ├── styles/
│   ├── utils/
│   ├── App.tsx
│   └── main.tsx
├── tests/
│   ├── unit/
│   ├── integration/
│   └── e2e/
├── .env.local
├── .env.example
├── .eslintrc.cjs
├── .prettierrc
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

---

## Getting Started Commands

```bash
# Create React app with Vite
npm create vite@latest riskpilot-frontend -- --template react-ts

# Install dependencies
npm install

# Install required packages
npm install -D tailwindcss postcss autoprefixer
npm install react-router-dom axios redux @reduxjs/toolkit react-redux
npm install react-hot-toast react-icons recharts date-fns zod
npm install @react-oauth/google

# Initialize Tailwind
npx tailwindcss init -p

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

---

## Estimated Development Timeline

- **Week 1**: Project setup, authentication implementation, basic layout
- **Week 2**: Dashboard and scan features (file, URL, email)
- **Week 3**: History management, results display, filtering
- **Week 4**: User profile, analytics, performance optimization, testing
- **Week 5**: Bug fixes, accessibility improvements, deployment

---

**Last Updated**: March 30, 2026  
**Status**: Ready for Frontend Development  
**Estimated Development Time**: 4-5 weeks for a team of 2-3 developers
