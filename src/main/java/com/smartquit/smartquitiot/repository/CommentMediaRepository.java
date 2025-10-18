package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.CommentMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentMediaRepository extends JpaRepository<CommentMedia, Integer> {
}
