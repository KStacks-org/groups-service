package com.kaustack.groups.dto.request;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.URL;

@Getter
@Setter
public class UpdateGroupRequest {
    @Size(min = 1, max = 3)
    private String section;

    @Size(min = 48, max = 48)
    @URL
    @Pattern(
            regexp = "^https://chat\\.whatsapp\\.com/.*$",
            message = "Invalid group link"
    )
    private String link;

    private Boolean generalGroup;

    private Boolean generalGroupMaleAndFemale;

    public void setLink(String link) {
        if (link != null) {
            link = link.trim();
            int queryIndex = link.indexOf('?');
            if (queryIndex != -1) {
                link = link.substring(0, queryIndex);
            }
        }
        this.link = link;
    }

    public boolean hasUpdates() {
        return section != null || link != null || generalGroup != null || generalGroupMaleAndFemale != null;
    }
}
