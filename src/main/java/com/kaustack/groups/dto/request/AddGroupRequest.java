package com.kaustack.groups.dto.request;

import com.kaustack.groups.exception.BusinessRuleViolationException;

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
    private Boolean generalGroup;

    @NotNull
    private Boolean generalGroupMaleAndFemale;

    // Custom setter for groupLink with sanitization
    public void setGroupLink(String groupLink) {
        if (groupLink != null) {
            // Remove leading/trailing whitespace
            groupLink = groupLink.trim();
            // Remove query parameters
            int queryIndex = groupLink.indexOf('?');
            if (queryIndex != -1) {
                groupLink = groupLink.substring(0, queryIndex);
            }
        }
        this.groupLink = groupLink;
    }

    // Validation method
    public void validate() {
        // If both checkboxes are false, section is required
        if (!generalGroup && !generalGroupMaleAndFemale) {
            if (section == null || section.isBlank()) {
                throw new BusinessRuleViolationException(
                    "Section is required when the group is not general"
                );
            }
        }

        // If generalGroupMaleAndFemale is true, generalGroup should be false
        if (generalGroupMaleAndFemale && generalGroup) {
            throw new BusinessRuleViolationException(
                "Cannot have both generalGroup and generalGroupMaleAndFemale set to true"
            );
        }
        
        // If generalGroup or generalGroupMaleAndFemale is true, section should be null
        if ((generalGroup || generalGroupMaleAndFemale) && section != null) {
            throw new BusinessRuleViolationException(
                "Section is not allowed when the group is general"
            );
        }
    }
}