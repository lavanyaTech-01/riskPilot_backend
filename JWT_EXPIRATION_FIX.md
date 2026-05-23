# JWT Expiration Error - Root Cause & Solution

## Problem Statement
You were getting the error:
```
io.jsonwebtoken.ExpiredJwtException: JWT expired 382558 milliseconds ago at 2026-04-06T12:42:08.000Z. 
Current time: 2026-04-06T12:48:30.558Z. Allowed clock skew: 0 milliseconds.
```

Even though a refresh token was being generated, the error still persisted because the access token validation was failing before the refresh token could be used.

## Root Cause Analysis

The issue had **three main problems**:

### 1. **No Clock Skew Tolerance**
- JWT parsing had `Allowed clock skew: 0 milliseconds`
- This means even tiny time differences between server and client would cause expiration errors
- Real-world systems have time synchronization variations

### 2. **Unhandled ExpiredJwtException in JwtFilter**
- When the access token expired, `JwtFilter.doFilterInternal()` tried to extract the token without catching `ExpiredJwtException`
- The exception propagated uncaught, causing a 500 or 401 error
- The client couldn't reach the `/refresh` endpoint to get a new token because the filter rejected the request

### 3. **Unhandled ExpiredJwtException in Refresh Endpoint**
- The `/refresh` endpoint didn't catch `ExpiredJwtException` when extracting the refresh token
- If the refresh token had expired, the endpoint would crash instead of returning a helpful error

## Solution Implemented

### Fix 1: Added Clock Skew to JwtUtil.java
**File**: `src/main/java/com/riskpilot/securityConfig/JwtUtil.java`

```java
// Clock skew in seconds to handle time synchronization issues
private static final long CLOCK_SKEW_SECONDS = 60;

private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(getKey())
        .clockSkewSeconds(CLOCK_SKEW_SECONDS)  // Added 60 second tolerance
        .build()
        .parseSignedClaims(token)
        .getPayload();
}
```

**Why this helps**: A 60-second clock skew tolerance allows for minor time synchronization differences between your server and client without rejecting valid tokens.

---

### Fix 2: Handle ExpiredJwtException in JwtFilter
**File**: `src/main/java/com/riskpilot/securityConfig/JwtFilter.java`

```java
if (authHeader != null && authHeader.startsWith("Bearer ")) {
    token = authHeader.substring(7);
    try {
        email = jwtService.extractToken(token);
    } catch (ExpiredJwtException e) {
        // Token is expired, allow the request to proceed without authentication
        System.out.println("JWT token is expired, allowing request to proceed without authentication");
        filterChain.doFilter(request, response);
        return;
    } catch (Exception e) {
        // Invalid token, allow the request to proceed
        System.out.println("Invalid JWT token: " + e.getMessage());
        filterChain.doFilter(request, response);
        return;
    }
}
```

**Why this helps**: 
- Now when an access token expires, the filter gracefully passes the request through
- The client can then call `/refresh` endpoint with their refresh token to get a new access token
- Prevents uncaught exceptions from crashing your application

---

### Fix 3: Handle ExpiredJwtException in Refresh Endpoint
**File**: `src/main/java/com/riskpilot/controller/AuthController.java`

```java
@PostMapping("/refresh")
public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
    try {
        String refreshToken = request.get("refreshToken");
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Refresh token is required"));
        }
        
        try {
            // Verify that it's actually a refresh token
            String tokenType = jwtService.getTokenType(refreshToken);
            if (tokenType == null || !tokenType.equals("REFRESH")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid refresh token"));
            }
            
            // Extract email and generate new access token
            String email = jwtService.extractToken(refreshToken);
            String newAccessToken = jwtService.generateTokenForOAuth2User(email);
            
            return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "message", "Token refreshed successfully!"
            ));
        } catch (ExpiredJwtException e) {
            // Refresh token itself has expired
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Refresh token has expired. Please login again."));
        }
    } catch (Exception ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "error", "message", "Invalid refresh token: " + ex.getMessage()));
    }
}
```

**Why this helps**:
- If refresh token expires, users get a helpful message asking them to login again
- No more uncaught exceptions when refresh token is invalid or expired
- Clear error messages for debugging

---

## Token Expiration Times

Based on your implementation:
- **Access Token**: 15 minutes (`1000 * 60 * 15` milliseconds)
- **Refresh Token**: 7 days (`1000 * 60 * 60 * 24 * 7` milliseconds)
- **Clock Skew**: 60 seconds

## How to Test

### Test 1: Access Token Expiration & Refresh
```bash
# 1. Login and get tokens
POST /api/auth/login
Body: { "email": "user@example.com", "password": "password" }
Response: { "accessToken": "...", "refreshToken": "..." }

# 2. Wait ~15 minutes or manually test with an old token

# 3. Try to access a protected endpoint with expired access token
GET /api/protected
Header: Authorization: Bearer <expired_access_token>
Result: Request goes through filter without error (no authentication)

# 4. Use refresh endpoint to get new access token
POST /api/auth/refresh
Body: { "refreshToken": "..." }
Response: { "accessToken": "<new_token>", "message": "Token refreshed successfully!" }

# 5. Use new access token to access protected endpoint
GET /api/protected
Header: Authorization: Bearer <new_access_token>
Result: Success with authentication
```

### Test 2: Expired Refresh Token
```bash
# If you manually set a refresh token to expired or wait 7 days:
POST /api/auth/refresh
Body: { "refreshToken": "<expired_refresh_token>" }
Response: { "status": "error", "message": "Refresh token has expired. Please login again." }
```

---

## Additional Notes

1. **Clock Skew Configuration**: Adjust `CLOCK_SKEW_SECONDS` if you experience frequent timeout issues due to server time drift
2. **Token Expiration Times**: You can adjust the token validity periods in `generateToken()` and `generateRefreshToken()` methods
3. **Security Considerations**: 
   - Keep refresh tokens secure (HTTP-only cookies recommended)
   - Always include token expiration checks on the frontend
   - Implement token rotation for enhanced security

---

## Files Modified

1. ✅ `JwtUtil.java` - Added clock skew and import for ExpiredJwtException
2. ✅ `JwtFilter.java` - Added exception handling for expired/invalid tokens
3. ✅ `AuthController.java` - Added exception handling in refresh endpoint

All changes are backward compatible and ready to use!
