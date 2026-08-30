package com.kaustack.groups.dto.response.catalog;

import lombok.Getter;

import java.util.List;

@Getter
public class CatalogSectionsResponse {
    private List<CatalogSectionData> data;
    private String status;
}
