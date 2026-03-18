package com.kaustack.groups.dto.response.catalog;

import lombok.Getter;

import java.util.UUID;

@Getter
public class CourseData {
    private UUID id;
    private String code;
    private String number;
    private String title;
    private int credits;
}
