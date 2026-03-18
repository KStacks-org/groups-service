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
                Gender.valueOf(jwt().extractGender())
        );

        if (groups.isEmpty()) {
            throw new ResourceNotFoundException("No groups found");
        }
        return groups;
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
