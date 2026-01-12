package com.smartquit.smartquitiot.service;

public interface ContentModerationService {
    boolean isTextToxic(String text);
    boolean isImageNsfw(String imageUrl);
    boolean isVideoNsfw(String videoUrl);
}