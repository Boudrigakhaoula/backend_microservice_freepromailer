package com.pfe.authservice.service;

import com.pfe.authservice.dto.request.*;
import com.pfe.authservice.dto.response.UserProfileResponse;
import com.pfe.authservice.entity.User;
import com.pfe.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class UserService {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    // ---- Self-service ----
    public UserProfileResponse getMyProfile() {
        return toResponse(getCurrentUser());
    }

    @Transactional
    public UserProfileResponse updateMyProfile(UserProfileRequest req) {
        User u = getCurrentUser();
        if (!u.getEmail().equals(req.getEmail()) && userRepo.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email already exists");
        u.setFirstName(req.getFirstName());
        u.setLastName(req.getLastName());
        u.setEmail(req.getEmail());
        return toResponse(userRepo.save(u));
    }

    @Transactional
    public void changeMyPassword(ChangePasswordRequest req) {
        User u = getCurrentUser();
        if (!encoder.matches(req.getCurrentPassword(), u.getPassword()))
            throw new RuntimeException("Current password is incorrect");
        if (!req.getNewPassword().equals(req.getConfirmPassword()))
            throw new RuntimeException("Passwords do not match");
        u.setPassword(encoder.encode(req.getNewPassword()));
        userRepo.save(u);
    }

    // ---- Admin ----
    public List<UserProfileResponse> getAllUsers() {
        return userRepo.findAll().stream().map(this::toResponse).toList();
    }

    public Page<UserProfileResponse> getAllUsers(Pageable p) {
        return userRepo.findAll(p).map(this::toResponse);
    }

    public UserProfileResponse getUserById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public UserProfileResponse updateUser(Long id, AdminUpdateUserRequest req) {
        User u = findById(id);
        if (!u.getEmail().equals(req.getEmail()) && userRepo.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email already exists");
        u.setFirstName(req.getFirstName()); u.setLastName(req.getLastName());
        u.setEmail(req.getEmail()); u.setRole(req.getRole()); u.setActive(req.getActive());
        return toResponse(userRepo.save(u));
    }

    @Transactional
    public void resetUserPassword(Long id, String newPassword) {
        User u = findById(id);
        u.setPassword(encoder.encode(newPassword));
        userRepo.save(u);
    }

    @Transactional
    public UserProfileResponse activateUser(Long id) {
        User u = findById(id); u.setActive(true); return toResponse(userRepo.save(u));
    }

    @Transactional
    public UserProfileResponse deactivateUser(Long id) {
        User u = findById(id);
        if ("ADMIN".equals(u.getRole().name()) && userRepo.countByRole(u.getRole()) <= 1)
            throw new RuntimeException("Cannot deactivate the last admin");
        u.setActive(false);
        return toResponse(userRepo.save(u));
    }

    @Transactional
    public void deleteUser(Long id) {
        User u = findById(id);
        if ("ADMIN".equals(u.getRole().name()) && userRepo.countByRole(u.getRole()) <= 1)
            throw new RuntimeException("Cannot delete the last admin");
        userRepo.delete(u);
    }

    public List<UserProfileResponse> searchUsers(String keyword) {
        return userRepo.searchByKeyword(keyword).stream().map(this::toResponse).toList();
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + email));
    }

    private User findById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    private UserProfileResponse toResponse(User u) {
        return UserProfileResponse.builder()
                .id(u.getId()).email(u.getEmail())
                .firstName(u.getFirstName()).lastName(u.getLastName())
                .role(u.getRole().name()).active(u.getActive())
                .createdAt(u.getCreatedAt()).updatedAt(u.getUpdatedAt())
                .build();
    }
}

