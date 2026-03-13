package com.pfe.authservice.controller;

import com.pfe.authservice.dto.request.*;
import com.pfe.authservice.dto.response.UserProfileResponse;
import com.pfe.authservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UserProfileRequest req) {
        return ResponseEntity.ok(userService.updateMyProfile(req));
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String,String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req) {
        userService.changeMyPassword(req);
        return ResponseEntity.ok(Map.of("message","Password changed successfully"));
    }
}

