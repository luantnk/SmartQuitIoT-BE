package com.smartquit.smartquitiot.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class SnapshotUploadRequest {
    List<String> imageUrls;
}
