package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    
    Optional<Account> findByUsername(String username);

    Optional<Account> findByRole(Role role);
}
