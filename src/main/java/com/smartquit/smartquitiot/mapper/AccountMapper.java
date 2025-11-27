package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.AccountDTO;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.enums.Role;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountDTO toAccountDTO(Account account) {
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setId(account.getId());
        accountDTO.setUsername(account.getUsername());
        accountDTO.setEmail(account.getEmail());
        accountDTO.setRole(account.getRole().name());
        accountDTO.setAccountType(account.getAccountType().name());
        accountDTO.setIsBanned(account.isBanned());
        accountDTO.setIsActive(account.isActive());
        accountDTO.setCreatedAt(account.getCreatedAt());
        accountDTO.setIsFirstLogin(account.isFirstLogin());

        return accountDTO;
    }

    public AccountDTO toAccountPostDTO(Account account){
        if(account == null) {
            return null;
        }
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setId(account.getId());
        accountDTO.setUsername(account.getUsername());
        if(account.getRole().equals(Role.MEMBER)) {
            accountDTO.setFirstName(account.getMember().getFirstName());
            accountDTO.setLastName(account.getMember().getLastName());
            accountDTO.setAvatarUrl(account.getMember().getAvatarUrl());
        }else if(account.getRole().equals(Role.COACH)) {
            accountDTO.setFirstName(account.getCoach().getFirstName());
            accountDTO.setLastName(account.getCoach().getLastName());
            accountDTO.setAvatarUrl(account.getCoach().getAvatarUrl());
        }

        return accountDTO;
    }
}
