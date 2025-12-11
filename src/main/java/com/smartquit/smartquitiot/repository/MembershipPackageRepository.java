package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.MembershipPackage;
import com.smartquit.smartquitiot.enums.MembershipPackageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipPackageRepository extends JpaRepository<MembershipPackage, Integer> {
    List<MembershipPackage> findByType(MembershipPackageType membershipPackageType);
}
