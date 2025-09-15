package com.example.gsm.services;

import com.example.gsm.entity.News;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface NewsService {

    News createNews(News news);

    Optional<News> updateNews(String id, News news);

    void deleteNews(String id);

    Page<News> getAllNews(int page, int size);

    Page<News> searchNews(String keyword, int page, int size);

    Optional<News> getById(String id);
}
