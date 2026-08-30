package com.kaustack.groups.dto.response.catalog;

import lombok.Getter;

import java.util.UUID;

@Getter
public class CatalogSectionData {
    private UUID id;
    private UUID courseId;
    private Integer crn;
    private String sectionCode;
    private String branch;
}
