package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {

//    @EntityGraph(attributePaths = {
//            "account",
//            "commentMedia",
//            "replies",
//            "replies.account",
//            "replies.commentMedia"
//    })
//    @Query("""
//        SELECT DISTINCT c
//        FROM Comment c
//        WHERE c.post.id = :postId
//          AND c.parent IS NULL
//        ORDER BY c.createdAt DESC
//    """)
//    List<Comment> findRootCommentsByPostId(@Param("postId") Integer postId);
//
//    @EntityGraph(attributePaths = {
//            "account",
//            "commentMedia",
//            "replies",
//            "replies.account",
//            "replies.commentMedia"
//    })
//    @Query("SELECT DISTINCT c FROM Comment c WHERE c.parent.id = :parentId ORDER BY c.createdAt ASC")
//    List<Comment> findRepliesByParentId(@Param("parentId") Integer parentId);

    // CommentRepository
    @EntityGraph(attributePaths = {"account", "commentMedia"})
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parent IS NULL ORDER BY c.createdAt DESC")
    List<Comment> findRootCommentsByPostId(@Param("postId") Integer postId);

    @Query("SELECT c FROM Comment c WHERE c.parent.id = :parentId ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentId(@Param("parentId") Integer parentId);

    int countByPostId(int postId);

}
