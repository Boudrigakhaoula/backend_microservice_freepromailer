package com.pfe.authservice.repository;

import com.pfe.authservice.entity.User;
import com.pfe.authservice.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(UserRole role);

    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.email) LIKE LOWER(CONCAT('%',:kw,'%'))
           OR LOWER(u.firstName) LIKE LOWER(CONCAT('%',:kw,'%'))
           OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%',:kw,'%'))
        ORDER BY u.createdAt DESC
        """)
    List<User> searchByKeyword(@Param("kw") String keyword);
}

