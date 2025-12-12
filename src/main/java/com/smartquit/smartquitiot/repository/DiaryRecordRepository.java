package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.DiaryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryRecordRepository extends JpaRepository<DiaryRecord, Integer> {

    Optional<DiaryRecord> findByDateAndMemberId(LocalDate date, Integer memberId);

    List<DiaryRecord> findByMemberId(Integer memberId);

    List<DiaryRecord> findByMemberIdOrderByDateDesc(Integer memberId);

    Optional<DiaryRecord> findTopByMemberIdOrderByDateDesc(Integer memberId);
    Optional<DiaryRecord> findTopByMemberIdAndHaveSmokedIsTrueOrderByDateAsc(Integer memberId);
}
