package com.example.gsm.controller;

import com.example.gsm.entity.News;
import com.example.gsm.services.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @PostMapping
    public ResponseEntity<News> createNews(@RequestBody News news) {
        return ResponseEntity.ok(newsService.createNews(news));
    }

    @PutMapping("/{id}")
    public ResponseEntity<News> updateNews(@PathVariable String id, @RequestBody News news) {
        return newsService.updateNews(id, news)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNews(@PathVariable String id) {
        newsService.deleteNews(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<News>> getAllNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(newsService.getAllNews(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<News> getById(@PathVariable String id) {
        return newsService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<News>> searchNews(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(newsService.searchNews(keyword, page, size));
    }
}
