# GitHub Setup & Push Instructions

## 📋 Summary of Changes

The RiskPilot backend is now ready to be pushed to GitHub with the following security measures in place:

### ✅ What Has Been Done

1. **Enhanced .gitignore** 
   - Excludes `src/main/resources/application.properties` (contains API keys, database credentials, OAuth2 secrets)
   - Excludes all environment files (`.env`, `.env.local`, etc.)
   - Excludes certificate files (`.pem`, `.key`, `.crt`, `.p12`, `.jks`)
   - Excludes IDE configuration files

2. **Created Comprehensive README.md**
   - Project overview and features
   - Technology stack details
   - Setup and installation instructions
   - API documentation with examples
   - Security considerations
   - Deployment guidelines
   - Contributing guidelines

3. **Created application.properties.example**
   - Template for configuration with all required fields
   - Comments explaining each configuration
   - Placeholders for sensitive values

4. **Initial Git Commit**
   - 75 files committed with 16,855 insertions
   - Main commit includes all source code, documentation, and configuration
   - Sensitive files properly excluded

---

## 🚀 Push to GitHub

### Step 1: Update Remote URL (If Needed)

If you need to update the GitHub repository URL:

```bash
cd C:\Users\Lenovo\eclipse-workspace\RiskPilot
git remote set-url origin https://github.com/YOUR_USERNAME/riskPilot_backend.git
```

Replace `YOUR_USERNAME` with your actual GitHub username.

### Step 2: Verify Remote Configuration

```bash
git remote -v
```

Should show:
```
origin  https://github.com/YOUR_USERNAME/riskPilot_backend.git (fetch)
origin  https://github.com/YOUR_USERNAME/riskPilot_backend.git (push)
```

### Step 3: Push to GitHub

#### Option A: Using HTTPS (Password/Token)

```bash
git push -u origin master
```

When prompted, enter your GitHub username and personal access token (not your password).

To create a personal access token:
1. Go to GitHub Settings → Developer settings → Personal access tokens
2. Click "Generate new token"
3. Select scopes: `repo` (full control of private repositories)
4. Copy the token and use it as your password

#### Option B: Using SSH (Recommended)

If you have SSH set up on your machine:

```bash
git push -u origin master
```

To set up SSH:
1. Generate SSH key: `ssh-keygen -t ed25519 -C "your.email@example.com"`
2. Add SSH key to GitHub account (Settings → SSH and GPG keys)
3. Update remote: `git remote set-url origin git@github.com:YOUR_USERNAME/riskPilot_backend.git`

---

## 📝 After Pushing to GitHub

### 1. Configure application.properties

Create your local configuration file:

```bash
# Copy the example file
copy src\main\resources\application.properties.example src\main\resources\application.properties

# Edit the file and add your credentials
notepad src\main\resources\application.properties
```

Fill in all the placeholders:
- `YOUR_DATABASE_PASSWORD_HERE` - Your PostgreSQL password
- `YOUR_GEMINI_API_KEY_HERE` - From https://aistudio.google.com/app/apikeys
- `YOUR_VIRUSTOTAL_API_KEY_HERE` - From https://www.virustotal.com/
- `YOUR_GOOGLE_CLIENT_ID_HERE` - From Google Cloud Console
- `YOUR_GOOGLE_CLIENT_SECRET_HERE` - From Google Cloud Console
- `YOUR_SUPER_SECRET_JWT_KEY_...` - Generate a strong random key

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The application will be available at: `http://localhost:8080`

---

## 🔒 Security Checklist

- ✅ `src/main/resources/application.properties` is in .gitignore
- ✅ All API keys are in placeholders in application.properties.example
- ✅ No credentials are stored in the repository
- ✅ README includes security best practices
- ✅ .gitignore excludes environment files and sensitive data

---

## 📚 Project Structure Overview

```
RiskPilot/
├── src/main/java/com/riskpilot/
│   ├── controller/          # REST API Controllers
│   │   ├── AiAnalysisController.java
│   │   ├── AuthController.java
│   │   └── HistoryController.java
│   ├── service/             # Business Logic
│   │   ├── AnalysisService.java
│   │   ├── UrlAnalysisService.java
│   │   ├── EmailAnalysisService.java
│   │   ├── DescriptionAnalysisService.java
│   │   └── HistoryService.java
│   ├── model/               # Entity Classes & DTOs
│   ├── repository/          # Data Access Layer
│   ├── securityConfig/      # Security Configuration
│   └── util/                # Utility Classes
├── src/main/resources/
│   ├── application.properties.example  # Configuration template
│   └── db/                  # Database scripts
├── .gitignore               # Git ignore rules
├── README.md                # Main documentation
├── pom.xml                  # Maven dependencies
└── SYSTEM_DESIGN.md         # Architecture details
```

---

## 🤝 For Team Members

### Cloning the Repository

```bash
git clone https://github.com/YOUR_USERNAME/riskPilot_backend.git
cd riskPilot_backend
```

### Setting Up Local Environment

1. Copy and configure application.properties:
   ```bash
   copy src\main\resources\application.properties.example src\main\resources\application.properties
   ```

2. Add your API keys and database credentials to the new file

3. Create PostgreSQL database:
   ```bash
   createdb riskPilot
   ```

4. Build and run:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

---

## 🐛 Troubleshooting

### Git Push Fails

**Error**: `error: failed to push some refs to 'https://github.com/...'`

**Solution**: Pull latest changes first:
```bash
git pull origin master
git push origin master
```

### Authentication Failed

**Error**: `fatal: Authentication failed`

**Solution**:
- For HTTPS: Use a personal access token instead of password
- For SSH: Check SSH key is added to ssh-agent: `ssh-add ~/.ssh/id_ed25519`

### Remote Not Configured

**Error**: `fatal: 'origin' does not appear to be a 'git' repository`

**Solution**: Add remote:
```bash
git remote add origin https://github.com/YOUR_USERNAME/riskPilot_backend.git
```

---

## ✨ Next Steps

1. **Push code to GitHub** using instructions above
2. **Set up GitHub Actions** (optional) for CI/CD
3. **Create issues and pull request templates**
4. **Add GitHub wiki** for additional documentation
5. **Configure branch protection rules** for main branches
6. **Set up code scanning** for security vulnerabilities

---

## 📞 Support

For questions or issues:
- Check the main [README.md](./README.md)
- Review [SYSTEM_DESIGN.md](./SYSTEM_DESIGN.md)
- Check existing GitHub issues
- Contact the development team

---

**Status**: ✅ Ready for GitHub
**Last Updated**: May 23, 2026
**Sensitive Files Excluded**: Yes
