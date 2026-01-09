package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Post;
import com.smartquit.smartquitiot.enums.PostStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Integer> {
    List<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Post> findAllByOrderByCreatedAtDesc();

    List<Post> findByStatusOrderByCreatedAtDesc(PostStatus status);

    @EntityGraph(attributePaths = {"account", "media"})
    @Query("SELECT p FROM Post p WHERE p.id = :postId")
    Post findPostWithMediaAndAccount(@Param("postId") Integer postId);


    @Query("SELECT p FROM Post p LEFT JOIN p.account a " +
            "WHERE lower(p.title) LIKE lower(concat('%', :query, '%')) " +
            "OR lower(p.description) LIKE lower(concat('%', :query, '%')) " +
            "OR lower(p.content) LIKE lower(concat('%', :query, '%')) " +
            "OR lower(a.username) LIKE lower(concat('%', :query, '%')) " +
            "ORDER BY p.createdAt DESC")
    List<Post> searchPosts(@Param("query") String query);

    List<Post> findByAccountId(Integer accountId);

}
