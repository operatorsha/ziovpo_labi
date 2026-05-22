package ru.operatorsha.ziovpolabi.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.operatorsha.ziovpolabi.security.JwtService;
import ru.operatorsha.ziovpolabi.user.Role;
import ru.operatorsha.ziovpolabi.user.UserAccount;
import ru.operatorsha.ziovpolabi.user.UserAccountRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class AuthService {

    private final UserAccountRepository users;
    private final UserSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserAccountRepository users,
            UserSessionRepository sessions,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new IllegalStateException("Email is already registered");
        }

        UserAccount user = new UserAccount();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        users.save(user);

        return Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole().name()
        );
    }

    @Transactional
    public TokenResponse login(LoginRequest request, String ipAddress, String userAgent) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password())
        );
        var user = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return createTokenPair(user, ipAddress, userAgent);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();
        if (!jwtService.isValidRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        var session = sessions.findByRefreshTokenHashAndStatus(hash(refreshToken), SessionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Refresh session is not active"));
        session.setStatus(SessionStatus.REFRESHED);

        return createTokenPair(session.getUser(), session.getIpAddress(), session.getUserAgent());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            return;
        }
        sessions.findByRefreshTokenHash(hash(request.refreshToken()))
                .ifPresent(session -> session.setStatus(SessionStatus.REVOKED));
    }

    @Transactional
    public void logoutAll(String email) {
        var user = users.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        sessions.revokeActiveSessions(user, SessionStatus.REVOKED);
    }

    private TokenResponse createTokenPair(UserAccount user, String ipAddress, String userAgent) {
        var principal = User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();

        String accessToken = jwtService.generateAccessToken(principal, user.getId());
        String refreshToken = jwtService.generateRefreshToken(principal, user.getId(), jwtService.generateSessionId());

        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshTokenHash(hash(refreshToken));
        session.setIssuedAt(Instant.now());
        session.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()));
        session.setStatus(SessionStatus.ACTIVE);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        sessions.save(session);

        return new TokenResponse(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationMs() / 1000,
                jwtService.getRefreshTokenExpirationMs() / 1000
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
