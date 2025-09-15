package com.example.news.service.impl;

import com.example.gsm.entity.News;
import com.example.gsm.entity.repository.NewsRepository;
import com.example.gsm.services.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;

    @Override
    public News createNews(News news) {
        news.setCreatedAt(LocalDateTime.now());
        news.setUpdatedAt(LocalDateTime.now());
        return newsRepository.save(news);
    }

    @Override
    public Optional<News> updateNews(String id, News news) {
        return newsRepository.findById(id).map(existing -> {
            existing.setTitle(news.getTitle());
            existing.setContent(news.getContent());
            existing.setAuthor(news.getAuthor());
            existing.setAvatar(news.getAvatar());
            existing.setUpdatedAt(LocalDateTime.now());
            return newsRepository.save(existing);
        });
    }

    @Override
    public void deleteNews(String id) {
        newsRepository.deleteById(id);
    }

    @Override
    public Page<News> getAllNews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return newsRepository.findAll(pageable);
    }

    @Override
    public Page<News> searchNews(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return newsRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(keyword, keyword, pageable);
    }

    @Override
    public Optional<News> getById(String id) {
        return newsRepository.findById(id);
    }
}
