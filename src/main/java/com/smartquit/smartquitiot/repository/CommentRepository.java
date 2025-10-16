package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Comment;
import com.smartquit.smartquitiot.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {

    // Lấy comment cha của post (trả về danh sách comment cha)
    List<Comment> findByPostAndParentIsNullOrderByCreatedAtAsc(Post post);
}
