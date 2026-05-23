# Testing Google OAuth2 in Postman

## Step 1: Get a Real Google ID Token

### Option A: Using Google OAuth Playground (Easiest for Testing)

1. Go to [Google OAuth 2.0 Playground](https://developers.google.com/oauthplayground)
2. In the top-right, click the ⚙️ **Settings icon**
3. Check **"Use your own OAuth credentials"**
4. Enter your Google OAuth credentials:
   - **Client ID**: Your Client ID from Google Cloud Console
   - **Client Secret**: Your Client Secret from Google Cloud Console
5. Click **Close**
6. On the left side, find **"Google OAuth2 API v2"** → Expand it
7. Select **"openid email profile"** scopes
8. Click **Authorize APIs**
9. Grant access when prompted
10. Click **Exchange authorization code for tokens**
11. Copy the **ID Token** from the response (the `id_token` field)

### Option B: Using Your Frontend (If Already Built)

If you have a frontend with Google Sign-In button:
1. Sign in with Google on your frontend
2. Open browser DevTools (F12)
3. Go to **Console** tab
4. Get the ID token from the Google response
5. Copy it to use in Postman

### Option C: Get Token from Google's Token Endpoint (Advanced)

```bash
curl -X POST https://oauth2.googleapis.com/token \
  -d "client_id=YOUR_CLIENT_ID.apps.googleusercontent.com" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "redirect_uri=http://localhost:8080/login/oauth2/code/google" \
  -d "grant_type=authorization_code" \
  -d "code=YOUR_AUTH_CODE"
```

---

## Step 2: Test in Postman

### Test Login with Google OAuth

1. **Open Postman**

2. **Create a new POST request**

3. **Set URL**: 
   ```
   http://localhost:8080/api/auth/login
   ```

4. **Set Headers**:
   ```
   Content-Type: application/json
   ```

5. **Set Body** (select `raw` → `JSON`):
   ```json
   {
     "googleToken": "PASTE_YOUR_GOOGLE_ID_TOKEN_HERE"
   }
   ```

6. **Click Send**

7. **Expected Response** (Success - 200 OK):
   ```json
   {
     "message": "OAuth Login successful!",
     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "user": {
       "email": "your_email@gmail.com",
       "name": "Your Name"
     }
   }
   ```

### Test Signup with Google OAuth

1. **Create a new POST request**

2. **Set URL**:
   ```
   http://localhost:8080/api/auth/signup
   ```

3. **Set Headers**:
   ```
   Content-Type: application/json
   ```

4. **Set Body** (select `raw` → `JSON`):
   ```json
   {
     "googleToken": "PASTE_YOUR_GOOGLE_ID_TOKEN_HERE"
   }
   ```

5. **Click Send**

6. **Expected Response** (Success - 201 Created):
   ```json
   {
     "id": 1,
     "name": "Your Name",
     "email": "your_email@gmail.com",
     "password": null,
     "googleToken": null
   }
   ```

---

## Step 3: Test with JWT Token for Authenticated Requests

After getting the JWT token from login/signup:

1. **Create a new request** to an authenticated endpoint (e.g., `/api/history/all`)

2. **Set URL**:
   ```
   http://localhost:8080/api/history/all
   ```

3. **Set Headers**:
   ```
   Authorization: Bearer YOUR_JWT_TOKEN_HERE
   ```

4. **Click Send**

5. Should return authenticated data ✅

---

## Postman Collection Template (Import This)

Create a file named `RiskPilot-OAuth2-Tests.json` and import it into Postman:

```json
{
  "info": {
    "name": "RiskPilot Google OAuth2",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "GET Login Info",
      "request": {
        "method": "GET",
        "url": "http://localhost:8080/api/auth/login",
        "header": []
      }
    },
    {
      "name": "GET Signup Info",
      "request": {
        "method": "GET",
        "url": "http://localhost:8080/api/auth/signup",
        "header": []
      }
    },
    {
      "name": "OAuth Login",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"googleToken\": \"PASTE_GOOGLE_TOKEN_HERE\"}"
        },
        "url": "http://localhost:8080/api/auth/login"
      }
    },
    {
      "name": "OAuth Signup",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"googleToken\": \"PASTE_GOOGLE_TOKEN_HERE\"}"
        },
        "url": "http://localhost:8080/api/auth/signup"
      }
    },
    {
      "name": "Traditional Login",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"email\": \"user@example.com\", \"password\": \"password123\"}"
        },
        "url": "http://localhost:8080/api/auth/login"
      }
    },
    {
      "name": "Traditional Signup",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"email\": \"user@example.com\", \"password\": \"password123\", \"name\": \"User Name\"}"
        },
        "url": "http://localhost:8080/api/auth/signup"
      }
    },
    {
      "name": "Test Authenticated Request",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer PASTE_JWT_TOKEN_HERE"
          }
        ],
        "url": "http://localhost:8080/api/history/all"
      }
    }
  ]
}
```

---

## Quick Testing Checklist

### For Google OAuth:

✅ **Test 1: Get OAuth Login Info (GET)**
```
GET http://localhost:8080/api/auth/login
Expected: JSON with endpoint info
```

✅ **Test 2: Login with Google Token (POST)**
```
POST http://localhost:8080/api/auth/login
Body: {"googleToken": "YOUR_TOKEN"}
Expected: JWT token in response
```

✅ **Test 3: Signup with Google Token (POST)**
```
POST http://localhost:8080/api/auth/signup
Body: {"googleToken": "YOUR_TOKEN"}
Expected: User created, JWT token returned
```

✅ **Test 4: Use JWT Token (GET)**
```
GET http://localhost:8080/api/history/all
Header: Authorization: Bearer JWT_TOKEN
Expected: Authenticated data
```

### For Traditional Auth (Comparison):

✅ **Test 5: Login with Email/Password**
```
POST http://localhost:8080/api/auth/login
Body: {"email": "user@example.com", "password": "password123"}
Expected: JWT token
```

---

## Common Issues & Solutions

### Issue 1: "Invalid Google token"
**Cause**: Token is expired or malformed
**Solution**: 
- Generate a fresh token from OAuth Playground
- Ensure token is complete (no truncation)
- Token should start with `eyJ...`

### Issue 2: "Username and password required"
**Cause**: Missing `googleToken` in body, falling back to traditional auth
**Solution**: 
- Ensure you're sending `googleToken` field
- Check JSON is properly formatted

### Issue 3: Token validation fails silently
**Cause**: Basic JWT parsing doesn't verify signature
**Solution**: 
- This is expected for development
- Production should implement signature verification

### Issue 4: CORS Errors
**Cause**: Frontend on different port
**Solution**: 
- For testing in Postman, no CORS issues
- For frontend testing, add CORS configuration to SecurityConfig

---

## Postman Environment Variables (Optional)

1. Create a Postman Environment
2. Add variables:
   - `base_url`: `http://localhost:8080`
   - `google_token`: `PASTE_YOUR_TOKEN`
   - `jwt_token`: (Will be set from response)

3. Use in requests:
   ```
   {{base_url}}/api/auth/login
   ```

---

## Scripts for Auto-Token Capture (Advanced)

Add this to the **Tests** tab of your "OAuth Login" request:

```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("jwt_token", jsonData.token);
    console.log("✅ JWT token saved to environment: " + jsonData.token);
}
```

Then use `{{jwt_token}}` in authenticated requests!

---

## Workflow for Testing

1. **Get Google Token** (OAuth Playground)
2. **Paste in Postman** (Login/Signup endpoint)
3. **Copy JWT Token** from response
4. **Use JWT** in authenticated requests (History, Scan, etc.)
5. **Test all endpoints** with the JWT token

That's it! 🚀
