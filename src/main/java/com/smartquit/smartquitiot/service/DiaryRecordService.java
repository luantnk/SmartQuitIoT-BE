package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.DiaryRecordRequest;
import com.smartquit.smartquitiot.dto.response.DiaryRecordDTO;

public interface DiaryRecordService {

    DiaryRecordDTO logDiaryRecord(DiaryRecordRequest request);
}
