package com.tikitaka.tikitaka.domain.card;

import com.tikitaka.tikitaka.domain.card.dto.CardCountResponseDTO;
import com.tikitaka.tikitaka.domain.card.dto.CardOppositeMemberProfileResponseDTO;
import com.tikitaka.tikitaka.domain.card.dto.CardResponseDTO;
import com.tikitaka.tikitaka.domain.card.dto.CardThumbnailResponseDTO;
import com.tikitaka.tikitaka.domain.card.entity.Card;
import com.tikitaka.tikitaka.domain.member.MemberRepository;
import com.tikitaka.tikitaka.domain.member.constant.Gender;
import com.tikitaka.tikitaka.domain.member.entity.Member;
import com.tikitaka.tikitaka.global.error.ErrorCode;
import com.tikitaka.tikitaka.global.error.exception.BadRequestException;
import com.tikitaka.tikitaka.global.error.exception.InternalServerException;
import com.tikitaka.tikitaka.global.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final MemberRepository memberRepository;

    /**
     * 내가 받았던 모든 카드 기록을 가져온다 (챗봇 썸네일용)
     */
    public List<CardThumbnailResponseDTO> findAllCard(Member authMember){
        return cardRepository.findAllByMember(authMember).stream().map(
                card -> {
                    Member targetMember = memberRepository.findById(card.getTargetMemberId())
                            .orElse(null);

                    if (targetMember == null) {
                        return null;
                    }
                    return CardThumbnailResponseDTO.of(card, targetMember);
                }
        ).collect(Collectors.toList());
    }

    /**
     * 내가 받았던 모든 카드 기록을 가져온다 (챗봇 썸네일)
     */
    public CardCountResponseDTO getRemainingCount(Member authMember) {
        long count = 3L - countCardByMemberAndCreatedAtBetween(authMember);
        if (count < 0) {
            return new CardCountResponseDTO(0L);
        }
        else return new CardCountResponseDTO(count);
    }

    /**
     * 추천 받았던 유저를 필터링한 랜덤 추천 카드를 만든다
     * */
    public CardThumbnailResponseDTO createRandomCard(Member authMember) {
        //이미 ACTIVE 한 카드가 있으면 에러
        if (cardRepository.existsByMemberAndIsActiveTrue(authMember)) {
            throw new BadRequestException(ErrorCode.ACTIVE_CARD_ALREADY_EXIST);
        }
        //하루에 세 장 이상 추천을 요청할 경우 에러
        if (countCardByMemberAndCreatedAtBetween(authMember) >= 3) {
            throw new BadRequestException(ErrorCode.CARD_LIMIT_EXCEED);
        }

        final Member member = memberRepository.findByMember(authMember)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        final Gender memberGender = member.getGender();
        final Long memberId = member.getId();

        List<Long> existTargetMemberIds = new ArrayList<>();
        existTargetMemberIds.add(member.getId());

        try {
            //이미 존재하는 카드에 담긴 유저 ID들 가져오기
            existTargetMemberIds.addAll(member.getCards().stream().map(Card::getTargetMemberId).collect(Collectors.toList()));

            //나를 가리키고 있는 카드를 추천받은 상태이며 활성화되어 있는 상대를 필터링
            existTargetMemberIds.addAll(cardRepository.findAllByTargetMemberIdAndIsActiveTrue(memberId)
                    .stream().map(card -> card.getMember().getId()).collect(Collectors.toList()));

            //매칭 상태에 존재하는 카드에 담긴 상대 유저 ID들 필터링
            existTargetMemberIds.addAll(member.getMatchesFrom().stream().map(match -> match.getFromMember().getId()).collect(Collectors.toList()));
            existTargetMemberIds.addAll(member.getMatchesTo().stream().map(match -> match.getToMember().getId()).collect(Collectors.toList()));
        } catch (Exception e) {
            throw new InternalServerException("멤버 데이터 오류");
        }

        ///// 임시 정책으로 제거합니다 //////
        //추천 받았던 적 없는 새로운 추천 상대 리스트
//        List<Member> newTargetMemberList = memberRepository.findByIdNotInAndGenderNotAndDetailNotNull(existTargetMemberIds, memberGender);
        ///// 임시 정책으로 제거합니다 //////

        ///// 임시 정책 (성별에 관계없이 가져옴) //////
        List<Member> newTargetMemberList = memberRepository.findByIdNotInAndDetailNotNull(existTargetMemberIds);
        ///// 임시 정책 (성별에 관계없이 가져옴) //////

        if (newTargetMemberList.isEmpty()) {
            throw new NotFoundException(ErrorCode.RANDOM_USER_NOT_FOUND);
        }
        //(0 ~ size) 사이의 랜덤 인덱스 멤버 추출
        final Member newTargetMember = newTargetMemberList.get(new Random().nextInt(newTargetMemberList.size()));

        Card newCard = Card.builder()
                .member(member)
                .targetMemberId(newTargetMember.getId())
                .isActive(true)
                .build();

        cardRepository.save(newCard);

        return CardThumbnailResponseDTO.of(newCard, newTargetMember);
    }

    /**
     * 현재 ACTIVE 한 카드를 모두 거절하고 INACTIVE 상태로 만든다
     * */
    public CardResponseDTO rejectCard(Member authMember) {
        Card activeCard = findByMemberAndIsActiveTrue(authMember);
        activeCard.disable();

        cardRepository.save(activeCard);

        return CardResponseDTO.of(activeCard);
    }

    /**
     * 랜덤 추천받은 상대의 프로필 카드를 가져오는 서비스 로직
     * ACTIVE 한 카드에만 접근 권한이 있음
     * */
    public CardOppositeMemberProfileResponseDTO getCardProfileById(Member authMember) {
        Card activeCard = findByMemberAndIsActiveTrue(authMember);
        Long targetId = activeCard.getTargetMemberId();

        Member oppositeMember = memberRepository.findById(targetId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        return CardOppositeMemberProfileResponseDTO.of(oppositeMember);
    }

    public List<CardResponseDTO> findAllDTOByMember(Member member) {
        return cardRepository.findAllDTOByMember(member);
    }

    public List<CardResponseDTO> findAllDTO() {
        return cardRepository.findAllDTO();
    }

    public Card findByMemberAndIsActiveTrue(Member member) {
        return cardRepository.findByMemberAndIsActiveTrue(member)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACTIVE_CARD_NOT_FOUND));
    }

    /**
     * 오늘 날짜를 기준으로 하루동안 받은 카드 개수 반환
     * */
    private long countCardByMemberAndCreatedAtBetween(Member member) {
        //서버 시간으로 00시00분부터 23시59분까지받은 카드 수 종합
        LocalDateTime startDatetime = LocalDateTime.of(LocalDate.now(), LocalTime.of(0,0,0));
        LocalDateTime endDatetime = LocalDateTime.of(LocalDate.now(), LocalTime.of(23,59,59));
        return cardRepository.countByMemberAndCreatedAtBetween(member, startDatetime, endDatetime);
    }
}
