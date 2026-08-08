package com.kaustack.groups.service;

import com.kaustack.groups.dto.request.*;
import com.kaustack.groups.dto.response.catalog.*;
import com.kaustack.groups.exception.*;
import com.kaustack.groups.model.Group;
import com.kaustack.groups.model.GroupType;
import com.kaustack.groups.repository.GroupRepository;
import com.kaustack.groups.security.AuthContext;
import com.kaustack.groups.security.AuthContextHolder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
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

        CourseData course = fetchCourse(request.getCourseId());

        if (groupRepository.existsByLink(request.getGroupLink())) {
            throw new BusinessRuleViolationException(
                    "A group with this link already exists");
        }

        if (request.getCrn() != null && groupRepository.existsByCrn(request.getCrn())) {
            throw new BusinessRuleViolationException(
                    "A group with this CRN already exists");
        }

        GroupType type = resolveType(request.getGroupType());
        String section = type.requiresSection() ? request.getSection() : null;

        checkDuplicateGroup(course.getId(), section, request.getCrn(), type, null);

        Group group = Group.builder()
                .courseId(course.getId())
                .userId(auth().userId())
                .link(request.getGroupLink())
                .crn(type.isBothGenders() ? null : request.getCrn())
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

    @Transactional
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

        GroupType effectiveType = request.getGroupType() != null
                ? resolveType(request.getGroupType())
                : group.getGroupType();

        String effectiveSection = effectiveType.requiresSection()
                ? (request.getSection() != null ? request.getSection() : group.getSection())
                : null;

        if (effectiveType.requiresSection() && (effectiveSection == null || effectiveSection.isBlank())) {
            throw new BusinessRuleViolationException(
                    "Section is required when the group is not general");
        }

        boolean linkChanged = request.getLink() != null && !request.getLink().equals(group.getLink());
        boolean crnChanged = request.getCrn() != null && !request.getCrn().equals(group.getCrn());
        boolean attributesChanged = effectiveType != group.getGroupType()
                || (effectiveSection != null ? !effectiveSection.equals(group.getSection()) : group.getSection() != null);

        if (!linkChanged && !crnChanged && !attributesChanged) {
            return group;
        }

        if (linkChanged) {
            if (groupRepository.existsByLink(request.getLink())) {
                throw new BusinessRuleViolationException("A group with this link already exists");
            }
            group.setLink(request.getLink());
        }

        if (crnChanged) {
            if (!effectiveType.isBothGenders()) {
                if (groupRepository.existsByCrn(request.getCrn())) {
                    throw new BusinessRuleViolationException("A group with this CRN already exists");
                }
                group.setCrn(request.getCrn());
            } else {
                throw new BusinessRuleViolationException(
                        "CRN is not allowed for general (non-gender-specific) groups");
            }
        }

        if (attributesChanged) {
            checkDuplicateGroup(group.getCourseId(), effectiveSection, request.getCrn(), effectiveType, group.getId());

            group.setGroupType(effectiveType);
            group.setSection(effectiveSection);
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

    private GroupType resolveType(GroupType requested) {
        String gender = auth().userGender().toUpperCase();
        boolean jwtMale = "MALE".equals(gender);

        if (requested.isMale() && !jwtMale) {
            throw new BusinessRuleViolationException(
                    "You can only create MALE groups as a male user");
        }
        if (requested.isFemale() && jwtMale) {
            throw new BusinessRuleViolationException(
                    "You can only create FEMALE groups as a female user");
        }
        return requested;
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
