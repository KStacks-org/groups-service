package com.kaustack.groups.model;

import jakarta.persistence.*;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
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

    @Column(nullable = false, updatable = false)
    private UUID courseId;

    @Column(nullable = false, updatable = false)
    private UUID userId;

    @Setter
    private String section;

    @Column(nullable = false, unique = true)
    @Setter
    private String link;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private GroupType groupType;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

}
