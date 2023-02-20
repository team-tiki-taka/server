package com.tikitaka.tikitaka.infra.notification.entity;

import com.tikitaka.tikitaka.infra.notification.constant.NotificationType;
import com.tikitaka.tikitaka.global.config.entity.BaseEntity;
import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "Notification")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Notification extends BaseEntity {
    @Id
    @Column(name = "not_id")
    @GeneratedValue
    private Long id;

//    /* 알림 주인 */
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "mem_id")
//    @JsonIgnore
//    private Member member;
    @Column(name = "mem_id")
    private Long memberId;

    @Column(name = "not_type")
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "not_title")
    private String title;

    @Column(name = "not_content")
    private String content;
}
