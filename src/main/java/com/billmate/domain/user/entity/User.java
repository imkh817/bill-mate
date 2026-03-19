package com.billmate.domain.user.entity;

import com.billmate.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, unique = true, nullable = false)
    private String slackUserId;

    @Column(length = 30, nullable = false)
    private String slackWorkspaceId;

    @Column(length = 100)
    private String displayName;

    @Column
    @Builder.Default
    private LocalTime notificationTime = LocalTime.of(9, 0);

    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void updateNotificationTime(LocalTime notificationTime) {
        this.notificationTime = notificationTime;
    }
}
