package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    Optional<Account> findByUsername(String username);
    Optional<Account> findByEmail(String email);
    Optional<Account> findByUsernameOrEmail(String username, String email);
    List<Account> findByRole(Role role);
    Optional<Account> findByResetToken(String resetToken);
    List<Account> findByRoleAndCreatedAtBetween(Role role, LocalDateTime start, LocalDateTime end);

}
