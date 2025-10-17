package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostMediaRepository extends JpaRepository<PostMedia, Integer> {
    void deleteAllByPostId(Integer postId);
}
