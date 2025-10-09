package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    
    Optional<Account> findByUsername(String username);

    @Query("SELECT a FROM Account a JOIN a.member m WHERE m.email = :email")
    Optional<Account> findByMemberEmail(@Param("email") String email);

    Optional<Account> findByRole(Role role);
}
