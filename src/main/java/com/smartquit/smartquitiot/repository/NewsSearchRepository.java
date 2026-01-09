package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.document.NewsDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsSearchRepository extends ElasticsearchRepository<NewsDocument, Integer> {

    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"title\", \"content\"], \"fuzziness\": \"AUTO\"}}")
    List<NewsDocument> searchByKeyword(String keyword);
}