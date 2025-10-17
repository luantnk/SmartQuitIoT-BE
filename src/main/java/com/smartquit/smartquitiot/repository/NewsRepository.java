package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NewsRepository extends JpaRepository<News, Integer> {

    List<News> findAllByOrderByCreatedAtDesc();

    @Query("SELECT n FROM News n WHERE LOWER(n.title) LIKE %:query% OR LOWER(n.content) LIKE %:query% ORDER BY n.createdAt DESC")
    List<News> searchNews(@Param("query") String query);

    @Query("SELECT n FROM News n ORDER BY n.createdAt DESC")
    List<News> findLatestNews(org.springframework.data.domain.Pageable pageable);
}
