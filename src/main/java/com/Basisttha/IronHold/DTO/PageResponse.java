package com.Basisttha.IronHold.DTO;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PageResponse<T> {
    private List<T> content;//actual files/data
    private int page;//offset = page x size
    private int size;
    private long totalElements;//total number of entries
    private int totalPages;
    private boolean last;//flag to know if current page is last or not
}
