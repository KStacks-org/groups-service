package com.kaustack.groups.dto.request;

import com.kaustack.groups.exception.BusinessRuleViolationException;
import com.kaustack.groups.model.GroupType;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.URL;

import java.util.UUID;

@Getter
@Setter
public class AddGroupRequest {
    @NotNull
    private UUID courseId;

    @Size(min = 1, max = 3)
    private String section;

    @NotBlank
    @Size(min = 48, max = 48)
    @URL
    @Pattern(
            regexp = "^https://chat\\.whatsapp\\.com/.*$",
            message = "Invalid group link"
    )
    private String groupLink;

    @NotNull
    private GroupType groupType;

    public void setGroupLink(String groupLink) {
        if (groupLink != null) {
            groupLink = groupLink.trim();
            int queryIndex = groupLink.indexOf('?');
            if (queryIndex != -1) {
                groupLink = groupLink.substring(0, queryIndex);
            }
        }
        this.groupLink = groupLink;
    }

    public void validate() {
        if (groupType.requiresSection()) {
            if (section == null || section.isBlank()) {
                throw new BusinessRuleViolationException(
                        "Section is required when the group is not general"
                );
            }
        } else if (section != null) {
            throw new BusinessRuleViolationException(
                    "Section is not allowed when the group is general"
            );
        }
    }
}
