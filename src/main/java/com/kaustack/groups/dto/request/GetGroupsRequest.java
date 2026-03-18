package com.kaustack.groups.dto.request;

import lombok.Getter;

import java.util.UUID;

@Getter
public class GetGroupsRequest {
    private UUID courseId;
}
