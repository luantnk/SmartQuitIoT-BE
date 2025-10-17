package com.smartquit.smartquitiot.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CommentUpdateRequest {
    private String content;
    private List<CommentCreateRequest.CommentMediaRequest> media;
}
