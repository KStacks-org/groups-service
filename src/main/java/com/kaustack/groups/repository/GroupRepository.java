package com.kaustack.groups.repository;

import com.kaustack.groups.model.Group;
import com.kaustack.groups.model.GroupType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    boolean existsByLink(String link);

    @Query("""
            SELECT g FROM Group g
            WHERE g.courseId = :courseId
              AND g.groupType IN :types
              AND (:section IS NULL OR g.section = :section)
              AND (:excludeId IS NULL OR g.id <> :excludeId)
            """)
    Optional<Group> findConflicting(
            @Param("courseId") UUID courseId,
            @Param("section") String section,
            @Param("types") Collection<GroupType> types,
            @Param("excludeId") UUID excludeId);

    @Query("SELECT g FROM Group g WHERE g.courseId = :courseId AND g.groupType IN :types")
    List<Group> findVisibleForTypes(
            @Param("courseId") UUID courseId,
            @Param("types") Collection<GroupType> types);
}
