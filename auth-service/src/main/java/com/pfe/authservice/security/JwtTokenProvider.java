package com.pfe.authservice.security;

import com.pfe.authservice.entity.User;
import com.pfe.authservice.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {
    @Autowired
    private UserRepository userRepository;
    @Value("${jwt.secret}")   private String jwtSecret;
    @Value("${jwt.expiration}") private long jwtExpiration;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Authentication auth) {
        UserDetails u = (UserDetails) auth.getPrincipal();

        // Récupérer l'utilisateur complet pour avoir son ID
        User user = userRepository.findByEmail(u.getUsername()).orElseThrow();

        Date now = new Date();
        return Jwts.builder()
                .subject(u.getUsername())
                .claim("userId", user.getId())     // ← AJOUTER l'ID
                .claim("role", u.getAuthorities().stream()
                        .findFirst().map(a -> a.getAuthority()).orElse("ROLE_CLIENT"))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtExpiration))
                .signWith(key(), Jwts.SIG.HS512)
                .compact();
    }

    public String getUsername(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean validate(String token) {
        try { Jwts.parser().verifyWith(key()).build().parseSignedClaims(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }
}
