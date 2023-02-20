package com.tikitaka.tikitaka.domain.match.dto;

import com.tikitaka.tikitaka.domain.member.constant.Gender;
import com.tikitaka.tikitaka.domain.member.entity.Member;
import com.tikitaka.tikitaka.domain.member.entity.MemberDetail;
import com.tikitaka.tikitaka.domain.recommend.entity.Recommend;
import com.tikitaka.tikitaka.global.error.ErrorCode;
import com.tikitaka.tikitaka.global.error.exception.InternalServerException;
import com.tikitaka.tikitaka.global.error.exception.NotFoundException;
import com.tikitaka.tikitaka.global.util.CustomStringUtil;
import lombok.*;

import java.util.List;

/**
 * 매칭된 상대의 휴대전화를 포함한 프로필을 반환하는 DTO 입니다
 * @author gengminy 221013
 * */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
public class MatchOpenProfileResponseDTO {
    private List<String> images;
    private String name;
    private int age;

    private String phone;
    private String address;
    private Gender gender;

    private String jobName;
    private String jobPart;
    private String jobLocation;

    private String eduName;
    private String eduMajor;
    private String eduLevel;

    private List<String> personalities;
    private String religion;
    private int height;
    private String smoke;
    private String drink;

    private String hobby;
    private String style;
    private String introduce;

    private String mbti;
    private Recommendation recommend;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    @ToString
    public static class Recommendation {
        private String name;
        private Gender gender;
        private List<String> appeals;

        private String jobName;
        private String jobPart;
        private String jobLocation;
        private String eduName;
        private String eduMajor;
        private String eduLevel;

        private String meet;
        private String period;
        private String appealDetail;
    }

    public static MatchOpenProfileResponseDTO of(Member targetMember) {
        MemberDetail detail = targetMember.getDetail();
        //디테일 없는 유저로 시도
        if (detail == null) {
            throw new NotFoundException(ErrorCode.USER_NOT_SIGNED_UP);
        }

        //추천받은 적 없는 유저를 가져오는 것을 시도
        if (targetMember.getRecommendReceived() == null){
            throw new NotFoundException(ErrorCode.RECOMMEND_NOT_FOUND);
        }

        try {
            for (Recommend recommend : targetMember.getRecommendReceived()) {
                //추천 상태 검증
                if (recommend.getSender() != null && recommend.getReceiver() != null) {
                    return MatchOpenProfileResponseDTO.builder()
                            .images(detail.getImages())
                            .name(targetMember.getName())
                            .phone(targetMember.getPhone())
                            .age(targetMember.getAge())
                            .address(detail.getAddress())
                            .jobName(targetMember.getJobName())
                            .jobPart(targetMember.getJobPart())
                            .jobLocation(targetMember.getJobLocation())
                            .eduName(targetMember.getEduName())
                            .eduMajor(targetMember.getEduMajor())
                            .eduLevel(targetMember.getEduLevel())
                            .gender(targetMember.getGender())
                            .personalities(detail.getPersonalities())
                            .religion(detail.getReligion())
                            .height(detail.getHeight())
                            .smoke(detail.getSmoke())
                            .drink(detail.getDrink())
                            .hobby(detail.getHobby())
                            .style(detail.getStyle())
                            .introduce(detail.getIntroduce())
                            .mbti(detail.getMbti())
                            .recommend(Recommendation.builder()
                                    .name(CustomStringUtil.hideName(recommend.getSenderName()))
                                    .gender(recommend.getSenderGender())
                                    .appeals(recommend.getReceiverAppeals())
                                    .appealDetail(recommend.getReceiverAppealDetail())
                                    .eduName(recommend.getSender().getEduName())
                                    .eduMajor(recommend.getSender().getEduMajor())
                                    .eduLevel(recommend.getSender().getEduLevel())
                                    .jobName(recommend.getSender().getJobName())
                                    .jobPart(recommend.getSender().getJobPart())
                                    .jobLocation(recommend.getSender().getJobLocation())
                                    .meet(recommend.getReceiverMeet())
                                    .period(recommend.getReceiverPeriod())
                                    .build())
                            .build();

                }
            }
        } catch (Exception e) {
            throw new InternalServerException(ErrorCode._INTERNAL_SERVER_ERROR);
        }
        throw new NotFoundException(ErrorCode.RECOMMEND_NOT_FOUND);
    }
}
