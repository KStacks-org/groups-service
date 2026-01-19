package com.kaustack.groups.model;

import jakarta.persistence.*;

import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "groups")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, unique = true)
    private UUID id;

    @Column(nullable = false, updatable = false, unique = true)
    private UUID courseId;

    @Column(nullable = false, updatable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    @Setter
    private String section;

    @Column(nullable = false, unique = true)
    @Setter
    private String link;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Gender gender;

    @Column(nullable = false, updatable = false)
    private Boolean generalGroup;

    @Column(nullable = false, updatable = false)
    private Boolean generalGroupMaleAndFemale;

}
