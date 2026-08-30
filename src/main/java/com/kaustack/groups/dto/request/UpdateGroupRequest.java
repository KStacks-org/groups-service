package com.kaustack.groups.dto.request;

import com.kaustack.groups.model.GroupType;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.URL;

import java.util.UUID;

@Getter
@Setter
public class UpdateGroupRequest {
    private UUID sectionId;

    @Size(min = 48, max = 48)
    @URL
    @Pattern(
            regexp = "^https://chat\\.whatsapp\\.com/.*$",
            message = "Invalid group link"
    )
    private String link;

    private GroupType groupType;

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
        return sectionId != null || link != null || groupType != null;
    }
}
