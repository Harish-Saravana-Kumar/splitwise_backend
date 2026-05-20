package com.splitwise.services;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.splitwise.dto.response.AuthResponse;
import com.splitwise.models.User;
import com.splitwise.models.enums.Role;
import com.splitwise.repositories.UserRepository;
import com.splitwise.utils.JwtUtil;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);
    private static final String GOOGLE_PROVIDER = "google";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ObjectProvider<FirebaseAuth> firebaseAuthProvider;

    @Transactional
    public AuthResponse authenticate(String firebaseIdToken) {
        FirebaseToken decodedToken = verifyToken(firebaseIdToken);

        String email = decodedToken.getEmail();
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firebase token does not contain an email");
        }

        String firebaseUid = decodedToken.getUid();
        String displayName = StringUtils.hasText(decodedToken.getName())
                ? decodedToken.getName()
                : email.substring(0, email.indexOf('@'));
        String picture = decodedToken.getPicture();

        User user = userRepository.findByProviderAndProviderId(GOOGLE_PROVIDER, firebaseUid)
                .or(() -> userRepository.findByEmail(email))
                .map(existingUser -> syncGoogleProfile(existingUser, firebaseUid, displayName, picture))
                .orElseGet(() -> createGoogleUser(email, firebaseUid, displayName, picture));

        User savedUser = userRepository.save(user);
        String jwtToken = jwtUtil.generateToken(savedUser.getEmail());

        return AuthResponse.builder()
                .token(jwtToken)
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .provider(savedUser.getProvider())
                .providerId(savedUser.getProviderId())
                .profilePicture(savedUser.getProfilePicture() != null
                        ? savedUser.getProfilePicture()
                        : savedUser.getAvatarUrl())
                .build();
    }

    private FirebaseToken verifyToken(String firebaseIdToken) {
        if (!StringUtils.hasText(firebaseIdToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firebase token is required");
        }

        try {
            FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
            if (firebaseAuth == null) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Firebase Admin SDK is not configured. Add src/main/resources/firebase/serviceAccountKey.json or set FIREBASE_SERVICE_ACCOUNT_PATH."
                );
            }

            return firebaseAuth.verifyIdToken(firebaseIdToken);
        } catch (FirebaseAuthException ex) {
            log.warn("Firebase token verification failed", ex);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Firebase token", ex);
        } catch (RuntimeException ex) {
            log.warn("Unexpected Firebase token verification failure", ex);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to verify Firebase token", ex);
        }
    }

    private User syncGoogleProfile(User user, String firebaseUid, String displayName, String picture) {
        user.setProvider(GOOGLE_PROVIDER);
        user.setProviderId(firebaseUid);

        if (!StringUtils.hasText(user.getName())) {
            user.setName(displayName);
        }

        if (!StringUtils.hasText(user.getProfilePicture()) && StringUtils.hasText(picture)) {
            user.setProfilePicture(picture);
        }

        if (!StringUtils.hasText(user.getAvatarUrl()) && StringUtils.hasText(picture)) {
            user.setAvatarUrl(picture);
        }

        if (!StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        }

        return user;
    }

    private User createGoogleUser(String email, String firebaseUid, String displayName, String picture) {
        return User.builder()
                .name(displayName)
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .avatarUrl(picture)
                .provider(GOOGLE_PROVIDER)
                .providerId(firebaseUid)
                .profilePicture(picture)
                .role(Role.MEMBER)
                .createdAt(LocalDateTime.now())
                .build();
    }
}