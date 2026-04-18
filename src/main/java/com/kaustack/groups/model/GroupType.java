package com.kaustack.groups.model;

import java.util.EnumSet;
import java.util.Set;

public enum GroupType {
    GENERAL,
    GENERAL_MALE_ONLY,
    GENERAL_FEMALE_ONLY,
    SECTION_MALE,
    SECTION_FEMALE;

    public boolean isGeneral() {
        return this == GENERAL || this == GENERAL_MALE_ONLY || this == GENERAL_FEMALE_ONLY;
    }

    public boolean requiresSection() {
        return this == SECTION_MALE || this == SECTION_FEMALE;
    }

    public boolean isBothGenders() {
        return this == GENERAL;
    }

    public boolean isMale() {
        return this == GENERAL_MALE_ONLY || this == SECTION_MALE;
    }

    public boolean isFemale() {
        return this == GENERAL_FEMALE_ONLY || this == SECTION_FEMALE;
    }

    public Set<GroupType> conflictsWith() {
        return switch (this) {
            case GENERAL -> EnumSet.of(GENERAL, GENERAL_MALE_ONLY, GENERAL_FEMALE_ONLY);
            case GENERAL_MALE_ONLY -> EnumSet.of(GENERAL, GENERAL_MALE_ONLY);
            case GENERAL_FEMALE_ONLY -> EnumSet.of(GENERAL, GENERAL_FEMALE_ONLY);
            case SECTION_MALE -> EnumSet.of(SECTION_MALE);
            case SECTION_FEMALE -> EnumSet.of(SECTION_FEMALE);
        };
    }
}
