package com.pfe.authservice.controller;

import com.pfe.authservice.dto.request.AdminUpdateUserRequest;
import com.pfe.authservice.dto.response.UserProfileResponse;
import com.pfe.authservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final UserService userService;

    @GetMapping("/all")
    public ResponseEntity<List<UserProfileResponse>> getAll() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping
    public ResponseEntity<Page<UserProfileResponse>> getAllPaged(Pageable p) {
        return ResponseEntity.ok(userService.getAllUsers(p));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserProfileResponse>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(userService.searchUsers(keyword));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfileResponse> update(
            @PathVariable Long id, @Valid @RequestBody AdminUpdateUserRequest req) {
        return ResponseEntity.ok(userService.updateUser(id, req));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Map<String,String>> resetPassword(
            @PathVariable Long id, @RequestBody Map<String,String> body) {
        String pwd = body.getOrDefault("newPassword", "");
        if (pwd.length() < 6) throw new RuntimeException("Password must be at least 6 chars");
        userService.resetUserPassword(id, pwd);
        return ResponseEntity.ok(Map.of("message","Password reset successfully"));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<UserProfileResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(userService.activateUser(id));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<UserProfileResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivateUser(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String,String>> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message","User deleted successfully"));
    }
}
