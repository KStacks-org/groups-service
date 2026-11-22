package com.kaustack.groups.service;

import com.kaustack.groups.dto.request.*;
import com.kaustack.groups.dto.response.catalog.*;
import com.kaustack.groups.exception.*;
import com.kaustack.groups.model.Group;
import com.kaustack.groups.model.GroupType;
import com.kaustack.groups.repository.GroupRepository;
import com.kaustack.groups.security.AuthContext;
import com.kaustack.groups.security.AuthContextHolder;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@RegisterReflectionForBinding({CatalogCourseResponse.class, CatalogSectionsResponse.class})
public class GroupService {
    private final GroupRepository groupRepository;
    private final RestTemplate restTemplate;

    private AuthContext auth() {
        return AuthContextHolder.get();
    }

    @Value("${catalog.service.url}")
    private String catalogServiceUrl;

    public Group addGroup(AddGroupRequest request) {
        request.validate();

        if (groupRepository.existsByLink(request.getGroupLink())) {
            throw new BusinessRuleViolationException(
                    "A group with this link already exists");
        }

        UUID courseId;
        UUID sectionId;
        String section;
        Integer crn;
        GroupType type;

        if (request.getSectionId() != null) {
            CatalogSectionData sec = fetchSection(request.getCourseId(), request.getSectionId());
            courseId = request.getCourseId();
            sectionId = sec.getId();
            section = sec.getSectionCode();
            crn = sec.getCrn();
            type = sectionType(sec);
            assertGenderAllowed(type);
        } else {
            CourseData course = fetchCourse(request.getCourseId());
            courseId = course.getId();
            sectionId = null;
            section = null;
            crn = null;
            type = resolveType(request.getGroupType());
        }

        if (crn != null && groupRepository.existsByCrn(crn)) {
            throw new BusinessRuleViolationException(
                    "A group with this CRN already exists");
        }

        checkDuplicateGroup(courseId, section, crn, type, null);

        Group group = Group.builder()
                .courseId(courseId)
                .userId(auth().userId())
                .link(request.getGroupLink())
                .crn(crn)
                .sectionId(sectionId)
                .section(section)
                .groupType(type)
                .build();

        return groupRepository.save(group);
    }

    public List<Group> getGroups(UUID courseId) {

        // It will return 404 if the course not found
        fetchCourse(courseId);

        String gender = auth().userGender().toUpperCase();
        List<GroupType> visible = "MALE".equals(gender)
                ? List.of(GroupType.GENERAL, GroupType.GENERAL_MALE_ONLY, GroupType.SECTION_MALE)
                : List.of(GroupType.GENERAL, GroupType.GENERAL_FEMALE_ONLY, GroupType.SECTION_FEMALE);

        List<Group> groups = groupRepository.findVisibleForTypes(courseId, visible);

        return groups;
    }

    public void deleteGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        if (!group.getUserId().equals(auth().userId())) {
            throw new UnauthorizedException("You are not allowed to delete this group");
        }

        groupRepository.delete(group);
    }

    public Group updateGroup(UUID id, UpdateGroupRequest request) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + id));

        if (!group.getUserId().equals(auth().userId())) {
            throw new UnauthorizedException(
                    "You are not authorized to update this group. Only the group creator can update it.");
        }

        if (!request.hasUpdates()) {
            throw new BusinessRuleViolationException("No fields to update");
        }

        if (request.getSectionId() != null && request.getGroupType() != null) {
            throw new BusinessRuleViolationException(
                    "groupType is derived from the section; omit it when sectionId is provided");
        }

        GroupType effectiveType = group.getGroupType();
        UUID effectiveSectionId = group.getSectionId();
        String effectiveSection = group.getSection();
        Integer effectiveCrn = group.getCrn();

        if (request.getSectionId() != null) {
            CatalogSectionData sec = fetchSection(group.getCourseId(), request.getSectionId());
            effectiveType = sectionType(sec);
            assertGenderAllowed(effectiveType);
            effectiveSectionId = sec.getId();
            effectiveSection = sec.getSectionCode();
            effectiveCrn = sec.getCrn();
        } else if (request.getGroupType() != null) {
            if (request.getGroupType().requiresSection()) {
                throw new BusinessRuleViolationException(
                        "sectionId is required for section groups");
            }
            effectiveType = resolveType(request.getGroupType());
            effectiveSectionId = null;
            effectiveSection = null;
            effectiveCrn = null;
        }

        boolean linkChanged = request.getLink() != null && !request.getLink().equals(group.getLink());
        boolean typeChanged = effectiveType != group.getGroupType();
        boolean sectionChanged = !Objects.equals(effectiveSection, group.getSection())
                || !Objects.equals(effectiveSectionId, group.getSectionId());
        boolean crnChanged = !Objects.equals(effectiveCrn, group.getCrn());

        if (!linkChanged && !typeChanged && !sectionChanged && !crnChanged) {
            return group;
        }

        if (linkChanged) {
            if (groupRepository.existsByLink(request.getLink())) {
                throw new BusinessRuleViolationException("A group with this link already exists");
            }
            group.setLink(request.getLink());
        }

        if (crnChanged && effectiveCrn != null && groupRepository.existsByCrn(effectiveCrn)) {
            throw new BusinessRuleViolationException("A group with this CRN already exists");
        }

        if (typeChanged || sectionChanged || crnChanged) {
            checkDuplicateGroup(group.getCourseId(), effectiveSection, effectiveCrn, effectiveType, group.getId());

            group.setGroupType(effectiveType);
            group.setSectionId(effectiveSectionId);
            group.setSection(effectiveSection);
            group.setCrn(effectiveCrn);
        }

        return groupRepository.save(group);
    }

    private CourseData fetchCourse(UUID courseId) {
        try {
            CatalogCourseResponse response = restTemplate.getForObject(
                    catalogServiceUrl + "/courses/{courseId}",
                    CatalogCourseResponse.class,
                    courseId);
            return response.getData();
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Fetch Course error: ", e);
            throw new ResourceNotFoundException("Course not found: " + courseId);
        }
    }

    private CatalogSectionData fetchSection(UUID courseId, UUID sectionId) {
        try {
            CatalogSectionsResponse response = restTemplate.getForObject(
                    catalogServiceUrl + "/courses/{courseId}/sections",
                    CatalogSectionsResponse.class,
                    courseId);
            List<CatalogSectionData> sections = response != null ? response.getData() : null;
            if (sections == null) {
                throw new ResourceNotFoundException("Section not found: " + sectionId);
            }
            return sections.stream()
                    .filter(s -> sectionId.equals(s.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Fetch Section error: ", e);
            throw new ResourceNotFoundException("Course not found: " + courseId);
        }
    }

    private GroupType sectionType(CatalogSectionData sec) {
        return "MALE".equals(sectionGender(sec.getBranch()))
                ? GroupType.SECTION_MALE
                : GroupType.SECTION_FEMALE;
    }

    private String sectionGender(String branch) {
        if (branch != null) {
            if (branch.contains("طالبات")) {
                return "FEMALE";
            }
            if (branch.contains("طلاب")) {
                return "MALE";
            }
        }
        throw new BusinessRuleViolationException(
                "Unable to determine section gender from catalog");
    }

    private GroupType resolveType(GroupType requested) {
        assertGenderAllowed(requested);
        return requested;
    }

    private void assertGenderAllowed(GroupType type) {
        boolean userMale = "MALE".equals(auth().userGender().toUpperCase());

        if (type.isMale() && !userMale) {
            throw new BusinessRuleViolationException(
                    "You can only create MALE groups as a male user");
        }
        if (type.isFemale() && userMale) {
            throw new BusinessRuleViolationException(
                    "You can only create FEMALE groups as a female user");
        }
    }

    private void checkDuplicateGroup(UUID courseId, String section, Integer crn, GroupType type, UUID excludeId) {
        String sectionFilter = type.requiresSection() ? section : null;
        Integer crnFilter = type.requiresSection() ? crn : null;

        groupRepository
                .findConflicting(courseId, sectionFilter, crnFilter, type.conflictsWith(), excludeId)
                .ifPresent(existing -> {
                    throw new BusinessRuleViolationException(conflictMessage(type, existing));
                });
    }

    private static String conflictMessage(GroupType requested, Group existing) {
        GroupType found = existing.getGroupType();
        if (requested.requiresSection() && found == requested) {
            return "A group for section '" + existing.getSection() + "' already exists";
        }
        if (requested == found) {
            return "A " + requested + " group already exists for this course";
        }
        return "A " + found + " group already exists for this course and conflicts with " + requested;
    }
}
