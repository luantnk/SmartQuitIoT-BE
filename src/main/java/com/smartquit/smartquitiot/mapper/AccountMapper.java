package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.AccountDTO;
import com.smartquit.smartquitiot.entity.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountDTO toAccountDTO(Account account) {
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setId(account.getId());
        accountDTO.setUsername(account.getUsername());
        accountDTO.setRole(account.getRole().name());
        accountDTO.setAccountType(account.getAccountType().name());
        accountDTO.setBanned(account.isBanned());
        accountDTO.setActive(account.isActive());
        accountDTO.setCreatedAt(account.getCreatedAt());
        accountDTO.setFirstLogin(account.isFirstLogin());

        return accountDTO;
    }
}
