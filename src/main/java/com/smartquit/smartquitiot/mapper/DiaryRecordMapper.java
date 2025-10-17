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
        diaryRecordDTO.setDate(diaryRecord.getDate());
        diaryRecordDTO.setHaveSmoked(diaryRecord.isHaveSmoked());
        diaryRecordDTO.setCigarettesSmoked(diaryRecord.getCigarettesSmoked());
        diaryRecordDTO.setTriggers(diaryRecord.getTriggers());
        diaryRecordDTO.setUseNrt(diaryRecord.isUseNrt());
        diaryRecordDTO.setMoneySpentOnNrt(diaryRecord.getMoneySpentOnNrt());
        diaryRecordDTO.setCravingLevel(diaryRecord.getCravingLevel());
        diaryRecordDTO.setMoodLevel(diaryRecord.getMoodLevel());
        diaryRecordDTO.setConfidenceLevel(diaryRecord.getConfidenceLevel());
        diaryRecordDTO.setAnxietyLevel(diaryRecord.getAnxietyLevel());
        diaryRecordDTO.setNote(diaryRecord.getNote());
        diaryRecordDTO.setConnectIoTDevice(diaryRecord.isConnectIoTDevice());
        if(diaryRecord.isConnectIoTDevice()){
            diaryRecordDTO.setSteps(diaryRecord.getSteps());
            diaryRecordDTO.setHeartRate(diaryRecord.getHeartRate());
            diaryRecordDTO.setSpo2(diaryRecord.getSpo2());
            diaryRecordDTO.setActivityMinutes(diaryRecord.getActivityMinutes());
            diaryRecordDTO.setRespiratoryRate(diaryRecord.getRespiratoryRate());
            diaryRecordDTO.setSleepDuration(diaryRecord.getSleepDuration());
            diaryRecordDTO.setSleepQuality(diaryRecord.getSleepQuality());
        }

        return diaryRecordDTO;
    }
}
