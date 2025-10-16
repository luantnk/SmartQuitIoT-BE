package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Integer> {
    List<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Post> findAllByOrderByCreatedAtDesc();

    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.account " +
            "LEFT JOIN FETCH p.media " +
            "LEFT JOIN FETCH p.comments c " +
            "LEFT JOIN FETCH c.account " +
            "LEFT JOIN FETCH c.commentMedia " +
            "LEFT JOIN FETCH c.replies " +
            "WHERE p.id = :postId")
    Post findPostDetailById(Integer postId);

}
