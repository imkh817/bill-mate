package com.billmate.slack.install;

import com.billmate.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "slack_installations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class SlackInstallation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, unique = true, nullable = false)
    private String teamId;

    @Column(length = 100)
    private String teamName;

    @Column(length = 200, nullable = false)
    private String botToken;

    @Column(length = 30)
    private String botUserId;

    @Column(length = 30)
    private String installedByUserId;
}
