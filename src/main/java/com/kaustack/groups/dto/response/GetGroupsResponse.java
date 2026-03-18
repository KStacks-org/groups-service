package com.kaustack.groups.dto.response;

import com.kaustack.groups.model.Group;

import java.util.List;
import java.util.UUID;

public record GetGroupsResponse(
        List<GroupItem> groups
) {
    public record GroupItem(
            UUID id,
            String section,
            String link,
            Boolean generalGroup,
            Boolean generalGroupMaleAndFemale
    ) {
        public static GroupItem from(Group group) {
            return new GroupItem(group.getId(), group.getSection(), group.getLink(), group.getGeneralGroup(), group.getGeneralGroupMaleAndFemale());
        }
    }

    public static GetGroupsResponse from(List<Group> groups) {
        List<GroupItem> groupItems = groups.stream()
                .map(GroupItem::from)
                .toList();
        return new GetGroupsResponse(groupItems);
    }
}