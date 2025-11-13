package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.NewsMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsMediaRepository extends JpaRepository<NewsMedia, Integer> {

    List<NewsMedia> findByNewsId(Integer id);

}
