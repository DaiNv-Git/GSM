package com.example.gsm.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private long total;        // tổng số record
    private int pageNumber;    // số trang hiện tại
    private int pageSize;      // số record/trang
    private List<T> items;     // danh sách kết quả
}

