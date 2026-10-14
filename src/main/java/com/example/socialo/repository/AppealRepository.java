package com.example.socialo.repository;

import com.example.socialo.model.Appeal;
import com.example.socialo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppealRepository extends JpaRepository<Appeal, Long> {

    List<Appeal> findAllByOrderByCreatedAtDesc();

    List<Appeal> findByStatus(Appeal.Status status);

    Optional<Appeal> findTopByUserOrderByCreatedAtDesc(User user);

    long countByStatus(Appeal.Status status);
}
