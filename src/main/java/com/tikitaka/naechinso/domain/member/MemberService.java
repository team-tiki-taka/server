package com.tikitaka.naechinso.domain.member;

import com.tikitaka.naechinso.domain.member.dto.*;
import com.tikitaka.naechinso.domain.member.entity.Member;
import com.tikitaka.naechinso.domain.member.entity.MemberDetail;
import com.tikitaka.naechinso.global.common.response.TokenResponseDTO;
import com.tikitaka.naechinso.global.config.security.MemberAdapter;
import com.tikitaka.naechinso.global.config.security.dto.JwtDTO;
import com.tikitaka.naechinso.global.config.security.jwt.JwtTokenProvider;
import com.tikitaka.naechinso.global.error.ErrorCode;
import com.tikitaka.naechinso.global.error.exception.BadRequestException;
import com.tikitaka.naechinso.global.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public List<MemberCommonResponseDTO> findAll() {
        List<MemberCommonResponseDTO> memberList = memberRepository.findAll().stream()
                .map(member -> MemberCommonResponseDTO.of(member)).collect(Collectors.toList());
        return memberList;
    }


    public MemberCommonResponseDTO createCommonMember(MemberCommonJoinRequestDTO dto) {

        //이미 존재하는 유저일 경우 400
        Optional<Member> checkMember = memberRepository.findByPhone(dto.getPhone());
        if(!checkMember.isEmpty()) {
            throw new BadRequestException(ErrorCode.USER_ALREADY_EXIST);
        }

        Member member = MemberCommonJoinRequestDTO.toCommonMember(dto);
        memberRepository.save(member);

        MemberCommonResponseDTO res = MemberCommonResponseDTO.of(member);
        return res;
    }

    public TokenResponseDTO login(String phone) {

        Member checkMember = memberRepository.findByPhone(phone)
                .orElseThrow(() -> new BadRequestException(ErrorCode.USER_NOT_FOUND));

        UsernamePasswordAuthenticationToken authenticationToken
                = new UsernamePasswordAuthenticationToken(new MemberAdapter(checkMember), "", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        TokenResponseDTO tokenResponseDTO
                = jwtTokenProvider.generateToken(new JwtDTO(phone));

        //리프레시 토큰 저장 로직 아래에
        return tokenResponseDTO;
    }


    public MemberDetailResponseDTO readDetail(Member member) {
//
//        Member checkMember = memberRepository.findByPhone(phone)
//                .orElseThrow(() -> new BadRequestException(ErrorCode.USER_NOT_FOUND));

        MemberDetailResponseDTO dto = MemberDetailResponseDTO.of(member);

        return dto;
    }

    public MemberDetailResponseDTO createDetail(Member authMember, MemberDetailJoinRequestDTO dto) {
        //영속성 유지를 위한 fetch
        Member member = memberRepository.findById(authMember.getId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        //detail 정보가 있으면 이미 가입한 회원
        if (member.getDetail() != null) {
            throw new BadRequestException(ErrorCode.USER_ALREADY_EXIST);
        }

        MemberDetail detail = MemberDetail.of(member, dto);
        memberDetailRepository.save(detail);
        return MemberDetailResponseDTO.of(detail);
    }

    public MemberCommonResponseDTO updateJob(Member authMember, MemberJobUpdateRequestDTO dto){
        //영속성 유지를 위한 fetch
        Member member = memberRepository.findById(authMember.getId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        member.setJob(dto);
        memberRepository.save(member);
        return MemberCommonResponseDTO.of(member);
    }

}
