package com.optician.backend.auth;

import com.optician.backend.model.UserAccount;
import com.optician.backend.model.UserRole;
import com.optician.backend.repository.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userAccountRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet e-mail.");
        }

        UserAccount account = UserAccount.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.CLIENT)
                .active(true)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(userAccountRepository.save(account)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginClient(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        return ResponseEntity.ok(authenticate(request, UserRole.CLIENT, servletRequest, servletResponse));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> loginAdmin(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        return ResponseEntity.ok(authenticate(request, UserRole.ADMIN, servletRequest, servletResponse));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthResponse account) {
            return ResponseEntity.ok(account);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private AuthResponse authenticate(
            LoginRequest request,
            UserRole expectedRole,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String cleanEmail = normalizeEmail(request.email());
        log.info("AuthAttempt: email={}, expectedRole={}", cleanEmail, expectedRole);

        Optional<UserAccount> existingAccountOpt = userAccountRepository.findByEmailIgnoreCase(cleanEmail);

        UserAccount account;

        if (existingAccountOpt.isPresent()) {
            account = existingAccountOpt.get();
            if (!Boolean.TRUE.equals(account.getActive())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce compte a été désactivé.");
            }
            if (account.getRole() != expectedRole) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle non autorisé pour cet espace.");
            }
            if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Adresse e-mail ou mot de passe incorrect.");
            }
        } else {
            // Seed demo accounts on first login attempt if missing
            if ("admin@optivision.tn".equals(cleanEmail) && expectedRole == UserRole.ADMIN) {
                account = UserAccount.builder()
                        .email(cleanEmail)
                        .fullName("Direction OptiVision Admin")
                        .passwordHash(passwordEncoder.encode(request.password()))
                        .role(UserRole.ADMIN)
                        .active(true)
                        .build();
                account = userAccountRepository.save(account);
            } else if ("client@optivision.tn".equals(cleanEmail) && expectedRole == UserRole.CLIENT) {
                account = UserAccount.builder()
                        .email(cleanEmail)
                        .fullName("Sophie Martin")
                        .passwordHash(passwordEncoder.encode(request.password()))
                        .role(UserRole.CLIENT)
                        .active(true)
                        .build();
                account = userAccountRepository.save(account);
            } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Adresse e-mail ou mot de passe incorrect.");
            }
        }

        AuthResponse response = toResponse(account);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                response,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())));
        SecurityContext context = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);

        log.info("AuthSuccess: email={}, role={}", response.email(), response.role());
        return response;
    }

    private AuthResponse toResponse(UserAccount account) {
        return new AuthResponse(account.getId(), account.getFullName(), account.getEmail(), account.getRole());
    }

    private String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
    }
}
