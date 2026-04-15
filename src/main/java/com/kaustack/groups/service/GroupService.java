package com.kaustack.groups.service;

import com.kaustack.groups.dto.request.*;
import com.kaustack.groups.dto.response.catalog.*;
import com.kaustack.groups.exception.*;
import com.kaustack.groups.model.Gender;
import com.kaustack.groups.model.Group;
import com.kaustack.groups.repository.GroupRepository;
import com.kaustack.groups.security.JwtContextHolder;
import com.kaustack.jwt.JwtUtils;

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

    // Spring Boot services are singleton so we need to do this workaround
    private JwtUtils jwt() {
        return new JwtUtils(JwtContextHolder.getToken());
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

        Gender groupGender = request.getGeneralGroupMaleAndFemale() ? Gender.UNKNOWN
                : Gender.valueOf(jwt().extractGender().toUpperCase());
        boolean isGeneral = request.getGeneralGroup() || request.getGeneralGroupMaleAndFemale();
        String section = isGeneral ? null : request.getSection();

        checkDuplicateGroup(course.getId(), section, groupGender, request.getGeneralGroup(),
                request.getGeneralGroupMaleAndFemale(), null);

        Group group = Group.builder()
                .courseId(course.getId())
                .userId(jwt().extractUserId())
                .link(request.getGroupLink())
                .gender(groupGender)
                .section(section)
                .generalGroup(request.getGeneralGroup())
                .generalGroupMaleAndFemale(request.getGeneralGroupMaleAndFemale())
                .build();

        return groupRepository.save(group);
    }

    public List<Group> getGroups(UUID courseId) {

        // It will return 404 if the course not found
        fetchCourse(courseId);

        List<Group> groups = groupRepository.findByCourseIdAndGenderOrGeneralForBoth(
                courseId,
                Gender.valueOf(jwt().extractGender()));
        return groups;
    }

    public void deleteGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        if (!group.getUserId().equals(jwt().extractUserId())) {
            throw new UnauthorizedException("You are not allowed to delete this group");
        }

        groupRepository.delete(group);
    }

    @Transactional
    public Group updateGroup(UUID id, UpdateGroupRequest request) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + id));

        JwtUtils jwtUtils = jwt();

        if (!group.getUserId().equals(jwtUtils.extractUserId())) {
            throw new UnauthorizedException(
                    "You are not authorized to update this group. Only the group creator can update it.");
        }

        if (!request.hasUpdates()) {
            throw new BusinessRuleViolationException("No fields to update");
        }

        Boolean effectiveGeneralGroupMaleAndFemale = request.getGeneralGroupMaleAndFemale() != null
                ? request.getGeneralGroupMaleAndFemale()
                : group.getGeneralGroupMaleAndFemale();
        Boolean effectiveGeneralGroup = request.getGeneralGroup() != null
                ? request.getGeneralGroup()
                : group.getGeneralGroup();
        Gender effectiveGender = effectiveGeneralGroupMaleAndFemale
                ? Gender.UNKNOWN
                : Gender.valueOf(jwtUtils.extractGender().toUpperCase());
        String effectiveSection = (effectiveGeneralGroup || effectiveGeneralGroupMaleAndFemale)
                ? null
                : (request.getSection() != null ? request.getSection() : group.getSection());

        if (effectiveGeneralGroup && effectiveGeneralGroupMaleAndFemale) {
            throw new BusinessRuleViolationException(
                    "Cannot have both generalGroup and generalGroupMaleAndFemale set to true");
        }

        if (!effectiveGeneralGroup && !effectiveGeneralGroupMaleAndFemale
                && (effectiveSection == null || effectiveSection.isBlank())) {
            throw new BusinessRuleViolationException(
                    "Section is required when the group is not general");
        }

        boolean linkChanged = request.getLink() != null && !request.getLink().equals(group.getLink());
        boolean attributesChanged = !effectiveGeneralGroup.equals(group.getGeneralGroup()) ||
                !effectiveGeneralGroupMaleAndFemale.equals(group.getGeneralGroupMaleAndFemale()) ||
                effectiveGender != group.getGender() ||
                (effectiveSection != null ? !effectiveSection.equals(group.getSection()) : group.getSection() != null);

        if (!linkChanged && !attributesChanged) {
            return group;
        }

        if (linkChanged) {
            if (groupRepository.existsByLink(request.getLink())) {
                throw new BusinessRuleViolationException("A group with this link already exists");
            }
            group.setLink(request.getLink());
        }

        if (attributesChanged) {
            checkDuplicateGroup(group.getCourseId(), effectiveSection, effectiveGender,
                    effectiveGeneralGroup, effectiveGeneralGroupMaleAndFemale, group.getId());

            group.setGeneralGroupMaleAndFemale(effectiveGeneralGroupMaleAndFemale);
            group.setGeneralGroup(effectiveGeneralGroup);
            group.setGender(effectiveGender);
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

    private void checkDuplicateGroup(UUID courseId, String section, Gender gender, Boolean generalGroup,
            Boolean generalGroupMaleAndFemale, UUID excludeId) {
        if (generalGroupMaleAndFemale) {
            if (groupRepository.existsDuplicateGeneralForBoth(courseId, excludeId)) {
                throw new BusinessRuleViolationException(
                        "A general group for both genders already exists for this course");
            }
        } else if (generalGroup) {
            if (groupRepository.existsDuplicateGeneralPerGender(courseId, gender, excludeId)) {
                throw new BusinessRuleViolationException(
                        "A general group for your gender already exists for this course");
            }
        } else if (section != null) {
            if (groupRepository.existsDuplicateSection(courseId, section, gender, excludeId)) {
                throw new BusinessRuleViolationException("A group for this section already exists");
            }
        }
    }
}
