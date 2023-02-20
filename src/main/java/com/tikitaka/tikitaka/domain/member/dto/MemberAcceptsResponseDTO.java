package com.tikitaka.tikitaka.domain.member.dto;

import com.tikitaka.tikitaka.domain.member.entity.Member;
import lombok.*;

/**
 * 동의 여부에 대한 응답 DTO
 * @author gengminy 221030
 * */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
public class MemberAcceptsResponseDTO {
    private boolean acceptsService;
    private boolean acceptsInfo;
    private boolean acceptsReligion;
    private boolean acceptsLocation;
    private boolean acceptsMarketing;
    private boolean acceptsPush;

    public static MemberAcceptsResponseDTO of(Member member){
        return MemberAcceptsResponseDTO.builder()
                .acceptsInfo(member.isAcceptsInfo())
                .acceptsLocation(member.isAcceptsLocation())
                .acceptsMarketing(member.isAcceptsMarketing())
                .acceptsReligion(member.isAcceptsReligion())
                .acceptsPush(member.isAcceptsPush())
                .acceptsService(member.isAcceptsService())
                .build();
    }
}
