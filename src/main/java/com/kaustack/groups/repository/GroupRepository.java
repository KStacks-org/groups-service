package com.kaustack.groups.repository;

import com.kaustack.groups.model.Gender;
import com.kaustack.groups.model.Group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
    Optional<Group> findById(UUID id);
    List<Group> findByUserId(String userId);
    List<Group> findByCourseAndGender(String courseId, Gender gender);
    boolean existsByLink(String link);
}

