package com.kaustack.groups.dto.response;

import com.kaustack.groups.model.Group;

import java.util.UUID;

public record AddGroupResponse(
        UUID id,
        String link
) {
    public static AddGroupResponse from(Group group) {
        return new AddGroupResponse(group.getId(), group.getLink());
    }
}

