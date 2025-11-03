package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.DiaryRecordRequest;
import com.smartquit.smartquitiot.dto.response.DiaryRecordDTO;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;

import java.util.List;
import java.util.Map;

public interface DiaryRecordService {

    GlobalResponse<DiaryRecordDTO> logDiaryRecord(DiaryRecordRequest request);
    List<DiaryRecordDTO> getDiaryRecordsForMember();
    DiaryRecordDTO getDiaryRecordById(Integer id);
    Map<String , Object> getDiaryRecordsCharts();
}
