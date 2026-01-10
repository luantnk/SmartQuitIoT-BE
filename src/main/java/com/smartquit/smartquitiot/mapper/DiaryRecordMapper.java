package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.DiaryRecordDTO;
import com.smartquit.smartquitiot.entity.DiaryRecord;
import org.springframework.stereotype.Component;

@Component
public class DiaryRecordMapper {

    public DiaryRecordDTO toDiaryRecordDTO(DiaryRecord diaryRecord) {
        if(diaryRecord == null)
            return null;
        DiaryRecordDTO diaryRecordDTO = new DiaryRecordDTO();
        diaryRecordDTO.setId(diaryRecord.getId());
        if (diaryRecord.getDate() != null) {
            diaryRecordDTO.setDate(diaryRecord.getDate().toString());
        }
        diaryRecordDTO.setHaveSmoked(diaryRecord.isHaveSmoked());
        diaryRecordDTO.setCigarettesSmoked(diaryRecord.getCigarettesSmoked());
        diaryRecordDTO.setTriggers(diaryRecord.getTriggers());
        diaryRecordDTO.setIsUseNrt(diaryRecord.isUseNrt());
        diaryRecordDTO.setMoneySpentOnNrt(diaryRecord.getMoneySpentOnNrt());
        diaryRecordDTO.setCravingLevel(diaryRecord.getCravingLevel());
        diaryRecordDTO.setMoodLevel(diaryRecord.getMoodLevel());
        diaryRecordDTO.setConfidenceLevel(diaryRecord.getConfidenceLevel());
        diaryRecordDTO.setAnxietyLevel(diaryRecord.getAnxietyLevel());
        diaryRecordDTO.setNote(diaryRecord.getNote());
        diaryRecordDTO.setIsConnectIoTDevice(diaryRecord.isConnectIoTDevice());
        diaryRecordDTO.setEstimatedNicotineIntake(diaryRecord.getEstimatedNicotineIntake());
        diaryRecordDTO.setReductionPercentage(diaryRecord.getReductionPercentage());
        if(diaryRecord.isConnectIoTDevice()){
            diaryRecordDTO.setSteps(diaryRecord.getSteps());
            diaryRecordDTO.setHeartRate(diaryRecord.getHeartRate());
            diaryRecordDTO.setSpo2(diaryRecord.getSpo2());
            diaryRecordDTO.setSleepDuration(diaryRecord.getSleepDuration());
        }

        return diaryRecordDTO;
    }

    public DiaryRecordDTO toListDiaryRecordDTO(DiaryRecord diaryRecord) {
        if(diaryRecord == null)
            return null;
        DiaryRecordDTO diaryRecordDTO = new DiaryRecordDTO();
        diaryRecordDTO.setId(diaryRecord.getId());
        if (diaryRecord.getDate() != null) {
            diaryRecordDTO.setDate(diaryRecord.getDate().toString());
        }
        diaryRecordDTO.setHaveSmoked(diaryRecord.isHaveSmoked());
        diaryRecordDTO.setEstimatedNicotineIntake(diaryRecord.getEstimatedNicotineIntake());
        diaryRecordDTO.setReductionPercentage(diaryRecord.getReductionPercentage());
        return diaryRecordDTO;
    }
}
