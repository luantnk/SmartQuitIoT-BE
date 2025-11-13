package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.FeedbackRequest;
import com.smartquit.smartquitiot.dto.response.FeedbackResponse;
import com.smartquit.smartquitiot.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedbackService {
    void createFeedback(int appointmentId, int memberAccountId, FeedbackRequest request);
    Page<FeedbackResponse> getFeedbacksByCoachId(int coachId, Pageable pageable);
    Page<FeedbackResponse> getFeedbacksForCoachAccount(int accountId, Pageable pageable);
}
