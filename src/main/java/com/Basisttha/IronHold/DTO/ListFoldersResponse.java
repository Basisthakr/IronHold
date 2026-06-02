package com.Basisttha.IronHold.DTO;

import java.util.List;

import com.Basisttha.IronHold.Model.Folder;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ListFoldersResponse {
    private List<Folder> folders;
}
