package com.likelion.likelioncrud.auth.application;

import com.likelion.likelioncrud.auth.JwtUtil;
import com.likelion.likelioncrud.auth.api.dto.request.LoginRequestDto;
import com.likelion.likelioncrud.auth.api.dto.request.SignupRequestDto;
import com.likelion.likelioncrud.auth.api.dto.request.TokenRefreshRequestDto;
import com.likelion.likelioncrud.auth.api.dto.response.LoginResponseDto;
import com.likelion.likelioncrud.auth.api.dto.response.TokenRefreshResponseDto;
import com.likelion.likelioncrud.auth.domain.RefreshToken;
import com.likelion.likelioncrud.auth.domain.RefreshTokenRepository;
import com.likelion.likelioncrud.common.exception.BusinessException;
import com.likelion.likelioncrud.common.response.code.ErrorCode;
import com.likelion.likelioncrud.member.domain.Member;
import com.likelion.likelioncrud.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 기본적으로 읽기 전용 트랜잭션 적용 (조회 성능 최적화)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;  // SecurityConfig에서 빈으로 등록한 BCryptPasswordEncoder
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

//    // application.yml의 jwt.refresh-expiration 값 (밀리초)
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // 회원가입
    @Transactional  // DB에 저장하는 작업이므로 쓰기 트랜잭션 적용
    public void signup(SignupRequestDto request) {

        // 1. 이메일 중복 체크
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL_EXCEPTION, ErrorCode.DUPLICATE_EMAIL_EXCEPTION.getMessage());
        }

        // 2. 비밀번호 BCrypt 암호화 후 Member 생성
        Member member = Member.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))  // 비밀번호 암호화
                .build();

        // 3. DB 저장
        memberRepository.save(member);
    }


    // 로그인
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {

        // 1. 이메일로 회원 조회 (없으면 예외 처리)
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND_BY_EMAIL_EXCEPTION, ErrorCode.MEMBER_NOT_FOUND_BY_EMAIL_EXCEPTION.getMessage()));

        // 2. 입력한 비밀번호와 암호화된 비밀번호 비교
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_EXCEPTION, ErrorCode.INVALID_PASSWORD_EXCEPTION.getMessage());
        }

        // 3. 인증 성공 → Access Token 발급
        String accessToken = jwtUtil.generateToken(member.getMemberId());

        // [과제] Refresh Token 발급 및 DB 저장
        // TODO (1): jwtUtil.generateRefreshToken()을 호출 후, refreshToken 문자열을 발급
        String refreshToken = jwtUtil.generateRefreshToken(member.getMemberId()); // 앞서 로그인 성공한 회원의 memberId를 넣어 토큰 발급함

        // TODO (2): 기존에 저장된 이 사용자의 refresh token을 먼저 삭제
        refreshTokenRepository.deleteByMemberId(member.getMemberId());
        // 삭제 이유: 같은 사용자가 다시 로그인하면 또 다른 토큰이 발급되고, 이는 보안 문제진다. 애초에 Refresh Token은 만료 시간도 길기 때문에, 이전 토큰들로 Access Token을 재발급 할수 있기 때문이다.
        // 그래서, 최근 로그인에서 발급된 토큰만을 유효하게 만들기 위해 삭제한다. memberId를 통해 해당 토큰 삭제 로직이다.

        // TODO (3): RefreshToken 엔티티를 빌더로 생성하고 DB에 저장
        //JwtUtil.java에서는 JWT 토큰을 문자열로 만드는 빌더과정이고, 현 클래스 빌더: DB에 저장할 Refresh Token 엔티티 객체를 만드는 빌더이다.
        //Refresh Token 엔티티: memberId, token, expiredAt 이런 컬럼이 들어간다.
        RefreshToken refreshTokenEntity = RefreshToken.builder() //생성된 RefreshToken값을 DB에 넣기 위해 빌드 하겠다.
                .memberId(member.getMemberId()) // memberId를 구하고, 지정
                .token(refreshToken)
                .expiredAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000)) //만료 시간은 현재 시간에 만료 시간을 더하겠다. 근데 우리가 설정한 값은 밀리단위, 1000으로 나눔
                .build(); //빌드 끝!
        refreshTokenRepository.save(refreshTokenEntity); //그리고 레포지토리에 저장하겠다.


        // 4. Access Token + Refresh Token 반환
        return new LoginResponseDto(accessToken, refreshToken);
    }

    // [과제] Access Token 재발급
    @Transactional
    public TokenRefreshResponseDto reissue(TokenRefreshRequestDto request) {

        // TODO (1): request에서 refreshToken 문자열을 꺼내기
        String refreshToken = request.refreshToken(); // 클라이언트가 보낸 Refresh Token 문자열을 RequestDto에서 꺼낸다.
        // JwtUtil.java 주석처럼, Refresh Token은 DB와 클라이언트 둘 다 지니고 있고, DB 검증은 이후 단계에서 하기때문에, 여기서는 요청 받은 토큰 문자열만 꺼낸다.

        // TODO (2): jwtUtil.validateToken()으로 refresh token의 서명/만료를 검증
        //jwtUtil.validateToken을 통해 토큰이 유효한지 검증한다. 만약 유효하지 않다면, 오류메세지를 던진다.
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN_EXCEPTION, ErrorCode.INVALID_REFRESH_TOKEN_EXCEPTION.getMessage());
        }


        // TODO (3): DB에서 refreshToken 문자열로 RefreshToken 엔티티를 조회
        // 2번 검증: JWT 자체 검증, 토큰 형식 정상인지, 우리 secret key로 서명된 토큰인지, 만료 시간이 지나지 않았는지 판단 즉, JWT로서 정상 토큰인가? 여부
        // 3번 검증: JWT 정상이어도, 서버가 인정하지 않는 토큰일 수도 있기 떄문에 DB와 한번 더 검증한다. 왜 하냐? DB에는 최신 토큰값이 저장되어있지만,
        // 예전 브라우저탭, 다른 기기등 클라이언트에 저장된 토큰값이 다를수있기 때문에 한번 더 검증한다.
            RefreshToken savedRefreshToken = refreshTokenRepository.findByToken(refreshToken) //문자열화된 토큰을 db에 있는 토큰과 비교하여 유효성을 검증한다.
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN_EXCEPTION, ErrorCode.INVALID_REFRESH_TOKEN_EXCEPTION.getMessage()));

        // TODO (4): refresh token에서 userId를 추출하고, 새로운 Access Token을 발급
        //           힌트: jwtUtil.getUserId(refreshToken), jwtUtil.generateToken(userId)
        Long userId = jwtUtil.getUserId(refreshToken); //Refresh Token의 subject에 저장된 userId를 꺼내 새로운 변수에 담는다.
        String newAccessToken = jwtUtil.generateToken(userId); // 꺼낸 userId로 새로운 Access Token 발급

        // 5. 새로 발급한 Access Token 반환
        return new TokenRefreshResponseDto(newAccessToken);
    }
}
