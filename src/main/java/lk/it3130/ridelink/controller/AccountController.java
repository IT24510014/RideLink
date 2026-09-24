package lk.it3130.ridelink.controller;

import jakarta.validation.Valid;
import lk.it3130.ridelink.dto.AccountResponse;
import lk.it3130.ridelink.dto.AuthResponse;
import lk.it3130.ridelink.dto.LoginRequest;
import lk.it3130.ridelink.dto.RegisterRequest;
import lk.it3130.ridelink.dto.UpdateProfileRequest;
import lk.it3130.ridelink.dto.UpdateStatusRequest;
import lk.it3130.ridelink.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = accountService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = accountService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable String id, Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        AccountResponse response = accountService.getAccountById(id, currentUserId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateProfile(@PathVariable String id,
                                                         @RequestBody UpdateProfileRequest request,
                                                         Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        AccountResponse response = accountService.updateProfile(id, request, currentUserId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> updateStatus(@PathVariable String id,
                                                        @Valid @RequestBody UpdateStatusRequest request,
                                                        Authentication authentication) {
        String adminUserId = authentication != null ? authentication.getName() : "ADMIN";
        AccountResponse response = accountService.updateStatus(id, request, adminUserId);
        return ResponseEntity.ok(response);
    }
}
