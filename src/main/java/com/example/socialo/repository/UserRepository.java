package com.example.socialo.repository;

import com.example.socialo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.email = :value OR u.phone = :value")
    Optional<User> findByEmailOrPhone(@Param("value") String value);

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE " +
           "u.role = com.example.socialo.model.User.Role.USER AND (" +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(u.lastName)  LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(u.username)  LIKE LOWER(CONCAT('%',:q,'%')))" )
    List<User> searchUsers(@Param("q") String query);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isBlocked = false")
    long countActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.isBlocked = true")
    long countBlockedUsers();

    // Role-based queries
    List<User> findByRole(User.Role role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") User.Role role);
}
