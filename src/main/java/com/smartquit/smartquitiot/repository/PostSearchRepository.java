package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.document.PostDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, Integer> {
    List<PostDocument> findByTitleContainingOrDescriptionContainingOrContentContaining(String title, String description, String content);

    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"title\", \"description\", \"content\"], \"fuzziness\": \"AUTO\"}}")
    List<PostDocument> searchByKeyword(String keyword);
}
