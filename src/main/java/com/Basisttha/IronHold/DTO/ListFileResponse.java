package com.Basisttha.IronHold.DTO;

import java.util.List;

import com.Basisttha.IronHold.Model.StoredFile;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ListFileResponse {
    List<StoredFile> list;

    public ListFileResponse() {
    }

    public ListFileResponse(List<StoredFile> list) {
        this.list = list;
    }
}
