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

    private UUID sectionId;

    @NotBlank
    @Size(min = 48, max = 48)
    @URL
    @Pattern(
            regexp = "^https://chat\\.whatsapp\\.com/.*$",
            message = "Invalid group link"
    )
    private String groupLink;

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
        if (sectionId != null) {
            if (groupType != null) {
                throw new BusinessRuleViolationException(
                        "groupType is derived from the section; omit it when sectionId is provided"
                );
            }
            return;
        }

        if (groupType == null) {
            throw new BusinessRuleViolationException(
                    "groupType is required for general groups"
            );
        }

        if (groupType.requiresSection()) {
            throw new BusinessRuleViolationException(
                    "sectionId is required for section groups"
            );
        }
    }
}
