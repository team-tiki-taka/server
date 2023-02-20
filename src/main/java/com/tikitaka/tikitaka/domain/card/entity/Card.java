package com.tikitaka.tikitaka.domain.card.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tikitaka.tikitaka.domain.member.entity.Member;
import com.tikitaka.tikitaka.global.config.entity.BaseEntity;
import lombok.*;

import javax.persistence.*;

/**
 * 멤버 공통 정보를 담당하는 엔티티입니다
 * @author gengminy 220924
 * */
@Entity
@Table(name = "Card")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class Card extends BaseEntity {
    @Id
    @Column(name = "car_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mem_id")
    @JsonIgnore
    private Member member;

    @Column(name = "car_target_mem_id")
    private Long targetMemberId;

    @Column(name = "car_is_active")
    @Builder.Default
    private Boolean isActive = false;

    public void disable() {
        this.isActive = false;
    }
}
