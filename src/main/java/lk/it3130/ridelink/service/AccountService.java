package lk.it3130.ridelink.service;

import lk.it3130.ridelink.config.JwtService;
import lk.it3130.ridelink.dto.AccountResponse;
import lk.it3130.ridelink.dto.AuthResponse;
import lk.it3130.ridelink.dto.LoginRequest;
import lk.it3130.ridelink.dto.ProfileDto;
import lk.it3130.ridelink.dto.RegisterRequest;
import lk.it3130.ridelink.dto.UpdateProfileRequest;
import lk.it3130.ridelink.dto.UpdateStatusRequest;
import lk.it3130.ridelink.exception.AccountNotFoundException;
import lk.it3130.ridelink.exception.AccountSuspendedException;
import lk.it3130.ridelink.exception.EmailAlreadyExistsException;
import lk.it3130.ridelink.exception.InvalidCredentialsException;
import lk.it3130.ridelink.exception.UnauthorizedActionException;
import lk.it3130.ridelink.model.Account;
import lk.it3130.ridelink.model.AccountStatus;
import lk.it3130.ridelink.model.AccountStatusHistory;
import lk.it3130.ridelink.model.Profile;
import lk.it3130.ridelink.model.Role;
import lk.it3130.ridelink.repository.AccountRepository;
import lk.it3130.ridelink.repository.AccountStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountStatusHistoryRepository accountStatusHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (accountRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email is already registered: " + normalizedEmail);
        }

        Role role = request.getRole() != null ? request.getRole() : Role.PASSENGER;

        Account account = Account.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(normalizedEmail)
                .phoneNumber(request.getPhoneNumber().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status(AccountStatus.ACTIVE)
                .profile(Profile.builder().build())
                .build();

        Account savedAccount = accountRepository.save(account);

        String token = jwtService.generateToken(savedAccount);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .account(mapToAccountResponse(savedAccount))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Account account = accountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (account.getStatus() == AccountStatus.SUSPENDED) {
            throw new AccountSuspendedException("Account is suspended. Please contact support.");
        } else if (account.getStatus() == AccountStatus.DEACTIVATED) {
            throw new AccountSuspendedException("Account has been deactivated.");
        }

        String token = jwtService.generateToken(account);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .account(mapToAccountResponse(account))
                .build();
    }

    public AccountResponse getAccountById(String id, String currentUserId, boolean isAdmin) {
        if (!isAdmin && !id.equals(currentUserId)) {
            throw new UnauthorizedActionException("You are not authorized to view this account profile");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + id));

        return mapToAccountResponse(account);
    }

    public AccountResponse updateProfile(String id, UpdateProfileRequest request, String currentUserId, boolean isAdmin) {
        if (!isAdmin && !id.equals(currentUserId)) {
            throw new UnauthorizedActionException("You are not authorized to update this account profile");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + id));

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            account.setFirstName(request.getFirstName().trim());
        }

        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            account.setLastName(request.getLastName().trim());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            account.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (request.getProfile() != null) {
            Profile currentProfile = account.getProfile() != null ? account.getProfile() : new Profile();
            ProfileDto profileDto = request.getProfile();

            if (profileDto.getDateOfBirth() != null) {
                currentProfile.setDateOfBirth(profileDto.getDateOfBirth());
            }
            if (profileDto.getGender() != null) {
                currentProfile.setGender(profileDto.getGender());
            }
            if (profileDto.getProfilePictureUrl() != null) {
                currentProfile.setProfilePictureUrl(profileDto.getProfilePictureUrl());
            }
            if (profileDto.getAddress() != null) {
                currentProfile.setAddress(profileDto.getAddress());
            }
            if (profileDto.getEmergencyContact() != null) {
                currentProfile.setEmergencyContact(profileDto.getEmergencyContact());
            }

            account.setProfile(currentProfile);
        }

        Account updatedAccount = accountRepository.save(account);
        return mapToAccountResponse(updatedAccount);
    }

    public AccountResponse updateStatus(String id, UpdateStatusRequest request, String changedBy) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + id));

        AccountStatus oldStatus = account.getStatus();
        AccountStatus newStatus = request.getStatus();

        AccountStatusHistory history = AccountStatusHistory.builder()
                .accountId(account.getId())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .reason(request.getReason())
                .changedAt(Instant.now())
                .build();

        accountStatusHistoryRepository.save(history);

        account.setStatus(newStatus);
        Account updatedAccount = accountRepository.save(account);

        return mapToAccountResponse(updatedAccount);
    }

    public AccountResponse mapToAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .email(account.getEmail())
                .phoneNumber(account.getPhoneNumber())
                .role(account.getRole())
                .status(account.getStatus())
                .profile(account.getProfile())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
