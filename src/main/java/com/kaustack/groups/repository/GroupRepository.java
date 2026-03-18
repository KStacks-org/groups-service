package com.kaustack.groups.repository;

import com.kaustack.groups.model.Gender;
import com.kaustack.groups.model.Group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
    List<Group> findByUserId(UUID userId);
    List<Group> findByCourseIdAndGender(UUID courseId, Gender gender);
    boolean existsByLink(String link);

    @Query("SELECT COUNT(g) > 0 FROM Group g WHERE g.courseId = :courseId AND g.generalGroupMaleAndFemale = true AND (:excludeId IS NULL OR g.id <> :excludeId)")
    boolean existsDuplicateGeneralForBoth(@Param("courseId") UUID courseId, @Param("excludeId") UUID excludeId);

    @Query("SELECT COUNT(g) > 0 FROM Group g WHERE g.courseId = :courseId AND g.generalGroup = true AND g.gender = :gender AND (:excludeId IS NULL OR g.id <> :excludeId)")
    boolean existsDuplicateGeneralPerGender(@Param("courseId") UUID courseId, @Param("gender") Gender gender, @Param("excludeId") UUID excludeId);

    @Query("SELECT COUNT(g) > 0 FROM Group g WHERE g.courseId = :courseId AND g.section = :section AND g.gender = :gender AND g.generalGroup = false AND g.generalGroupMaleAndFemale = false AND (:excludeId IS NULL OR g.id <> :excludeId)")
    boolean existsDuplicateSection(@Param("courseId") UUID courseId, @Param("section") String section, @Param("gender") Gender gender, @Param("excludeId") UUID excludeId);
}

