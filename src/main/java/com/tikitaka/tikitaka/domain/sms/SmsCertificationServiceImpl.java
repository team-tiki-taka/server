package com.tikitaka.tikitaka.domain.sms;

import com.tikitaka.tikitaka.domain.member.MemberRepository;
import com.tikitaka.tikitaka.domain.member.entity.Member;
import com.tikitaka.tikitaka.domain.recommend.RecommendService;
import com.tikitaka.tikitaka.domain.recommend.dto.RecommendReceiverDTO;
import com.tikitaka.tikitaka.domain.sms.dto.SmsCertificationSuccessResponseDTO;
import com.tikitaka.tikitaka.global.common.response.TokenResponseDTO;
import com.tikitaka.tikitaka.global.config.security.dto.JwtDTO;
import com.tikitaka.tikitaka.global.config.security.jwt.JwtTokenProvider;
import com.tikitaka.tikitaka.global.error.ErrorCode;
import com.tikitaka.tikitaka.global.error.exception.InternalServerException;
import com.tikitaka.tikitaka.infra.sms.NaverSmsService;
import com.tikitaka.tikitaka.domain.sms.dto.SmsCertificationRequestDTO;
import com.tikitaka.tikitaka.global.error.exception.BadRequestException;
import com.tikitaka.tikitaka.global.error.exception.UnauthorizedException;
import com.tikitaka.tikitaka.global.config.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCertificationServiceImpl implements SmsCertificationService {
    private final NaverSmsService naverSmsService;
    private final RedisService redisService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final RecommendService recommendService;
    private final String VERIFICATION_PREFIX = "sms:";
    private final int VERIFICATION_TIME_LIMIT = 3 * 60;

    @Value("${spring.profiles.active}")
    private String springProfile;

    /**
     * 인증번호가 담긴 메세지를 전송한다
     * @param to 수신자
     * @return 전송 성공시 메세지
     */
    @Override
    public String sendVerificationMessage(String to) {
        //랜덤 6자리 인증번호
        final String verificationCode = generateVerificationCode();
        //3분 제한시간
        final Duration verificationTimeLimit = Duration.ofSeconds(VERIFICATION_TIME_LIMIT);

        //[local, dev] 배포 환경이 아닐때는 fake service 를 제공합니다
        if (springProfile == null || !springProfile.equals("prod")) {
            log.info("스프링 프로파일(" + springProfile + ") 따라 fake 서비스를 제공합니다");
            String message = generateMessageWithCode(verificationCode);
            log.info(message);
            redisService.setValues(VERIFICATION_PREFIX + to, verificationCode, verificationTimeLimit);
            return message;
        }

        //[prod] 실 배포 환경에서는 문자를 전송합니다
        try {
            //네이버 sms 메세지 dto
            if (naverSmsService.sendMessage(to, generateMessageWithCode(verificationCode))) {
                //전송 성공하면 redis 에 3분 제한의 인증번호 토큰 저장
                redisService.setValues(VERIFICATION_PREFIX + to, verificationCode, verificationTimeLimit);
                return "메세지 전송 성공";
            } else {
                throw new BadRequestException(ErrorCode._BAD_REQUEST, "메세지 전송에 실패하였습니다");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException(ErrorCode._BAD_REQUEST, "메세지 발송에 실패하였습니다");
        }
    }

    /**
     * 인증번호를 검증한다
     * @param requestDTO {phoneNumber: 휴대폰 번호, code: 인증번호}
     * @return TokenResponseDTO { accessToken, refreshToken }
     */
    @Override
    public SmsCertificationSuccessResponseDTO verifyCode(SmsCertificationRequestDTO requestDTO) {
        String phoneNumber = requestDTO.getPhoneNumber();
        String code = requestDTO.getCode();
        String key = VERIFICATION_PREFIX + phoneNumber;


        /** *********** 임시 정책으로 제거 ********* */
        if (springProfile == null || springProfile.equals("prod")) {
            ///////////////////////////////////
            ///////////////////////////////////
            ///////////////////////////////////

            //redis 에 해당 번호의 키가 없는 경우는 인증번호(3분) 만료로 처리
            if (!redisService.hasKey(key)) {
                throw new UnauthorizedException(ErrorCode.EXPIRED_VERIFICATION_CODE);
            }

            //redis 에 해당 번호의 키와 인증번호가 일치하지 않는 경우
            if (!redisService.getValues(key).equals(code)) {
                throw new UnauthorizedException(ErrorCode.MISMATCH_VERIFICATION_CODE);
            }
        }
        /** ******************** */

        //redis 인증 필터 성공하면
        try {
            //redis 에서 번호 제거
            redisService.deleteValues(key);

            //유효한 추천인이 있는 추천사 리스트
            List<RecommendReceiverDTO> recommendReceivedList
                    = recommendService.findAllRecommendReceivedListBasicByPhone(phoneNumber);

            //가입 안된 회원일 경우 registerToken 리턴
            Optional<Member> checkMember = memberRepository.findByPhone(phoneNumber);
            if (checkMember.isEmpty()) {
                String registerToken = jwtTokenProvider.generateRegisterToken(new JwtDTO(phoneNumber));

                return SmsCertificationSuccessResponseDTO.builder()
                        .registerToken(registerToken)
                        .recommendReceived(recommendReceivedList)
                        .build();
            }


            //이미 가입한 회원이면
            //인증한 휴대폰 번호로 로그인 후 토큰 생성
            final Member authMember = checkMember.get();
            TokenResponseDTO tokenResponseDTO
                    = jwtTokenProvider.generateToken(new JwtDTO(phoneNumber, authMember.getRole().getDetail()));

            //가입 여부 (detail == null) 확인 후 받은 추천서 꺼내옴
            final Boolean hasDetail = authMember.getDetail() != null;
            //유저 밴 여부 가져오기
            final Boolean isBanned = false;

            //액세스 + 리프레시 토큰 반환
            return SmsCertificationSuccessResponseDTO.builder()
                    .accessToken(tokenResponseDTO.getAccessToken())
                    .refreshToken(tokenResponseDTO.getRefreshToken())
                    .recommendReceived(recommendReceivedList)
                    .isActive(hasDetail)
                    .isBanned(false)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalServerException("jwt 토큰 생성 에러");
        }
    }

    /**
     * 랜덤 인증번호를 생성한다
     * @return 인증번호 6자리
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int verificationCode = random.nextInt(888888) + 111111;
        return Integer.toString(verificationCode);
    }

    /**
     * 인증번호가 포함된 메세지를 생성한다
     * @param code 인증번호 6자리
     * @return 인증번호 6자리가 포함된 메세지
     */
    private String generateMessageWithCode(String code) {
        final String provider = "티키타카";
        return "[" + provider + "] 인증번호 [" + code + "] 를 입력해주세요 :)";
    }
}
