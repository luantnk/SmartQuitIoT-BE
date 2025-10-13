package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.entity.MembershipPackage;
import com.smartquit.smartquitiot.repository.MembershipPackageRepository;
import com.smartquit.smartquitiot.service.MembershipPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipPackageServiceImpl implements MembershipPackageService {

    private final MembershipPackageRepository membershipPackageRepository;

    @Override
    public List<MembershipPackage> getMembershipPackages() {
        return membershipPackageRepository.findAll();
    }
}
