package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.FeedbackRequest;
import com.smartquit.smartquitiot.entity.Feedback;

public interface FeedbackService {
    void createFeedback(int appointmentId, int memberAccountId, FeedbackRequest request);
}
