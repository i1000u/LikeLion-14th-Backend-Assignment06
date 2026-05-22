package com.likelion.likelioncrud.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
    //메모장 써도 되긴하는데, 공부한거 어필하고 싶어서 적어봤음 헤헷.. 이상한거면 지우고 다시 올릴게요
    //시간 단위: ms(밀리 초) -> 604800초, 7일로 설정: Refresh Token은 Access Token 시간 만료 시, 재발급용이기 때문에 길어야함.
    // Access Token: 보안 문제 때문에, 만료시간을 짧게 잡는다. 그럼 Refresh Token은 탈취당해도 문제가 없나? -> 문제 커짐 쉽지 않음
    // 나름 방어 방법을 여러가지 취한다.
    //1.DB에 동시 저장하여, 서버가 통제한다. (클라이언트에게 온전히 맡기는 것이 아님을 의미)
    //2. 재로그인 시, 기존 Refresh Token 삭제/재발급
    //3. 로그아웃 시, Refresh Token 삭제 .. 이러한 방법을 통해 나름 토큰 지키기 모드 한다.

    //jwt는 .으로 나뉜 3부분으로 구성된다.  header.payload.signature -> ex) xxxx.yyyy.zzzz 이런 식이다.
    //Header: 이 토큰이 어떤 방식으로 서명됐는지 정보, Payload: 실제 담고싶은 정보(발급/만료 시간, userId 등) , Signature: header + payload를 secret key로 서명한 값, 조작 여부 확인용

    // application.yml의 jwt.secret 값을 자동으로 주입
    @Value("${jwt.secret}")
    private String secretKey;

    // application.yml의 jwt.expiration 값을 자동으로 주입
    @Value("${jwt.expiration}")
    private long expiration;

    // application.yml의 jwt.refresh-expiration 값을 자동으로 주입
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // Access Token 생성
    public String generateToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))                                       // payload에 userId 저장 (subject는 String 타입)
                .issuedAt(new Date())                                                  // 토큰 발급 시간
                .expiration(new Date(System.currentTimeMillis() + expiration))         // 토큰 만료 시간
                .signWith(getSigningKey())                                             // Secret Key로 서명
                .compact();                                                            // 토큰 문자열로 변환
    }

    // TODO: [과제] Refresh Token 생성
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()            // JWT를 만들기 위한 빌더 (빌더: 아래 여러 필요 정보 생성자를 넣으면 읽기 불편하기 때문에 빌더 패던을 통해 편하게 넣을 수 있음 )
                .subject(String.valueOf(userId))            // userId는 long타입이므로, 이를 문자열 타입로 바꾼 후, subject에 사용자 id 넣음, 통상, jwt에서 subject == 토큰의 주인 의미
                .issuedAt(new Date())               //토큰이 발급된 시간을 넣는다. (토큰 언제 만들어졌지? 이거 정보넣는거임)
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))        // 토큰 만료 시간 지정하는거 이름부터 딱봐도 유통기한, 현재 시간 + 지정한 만료시간 주입
                .signWith(getSigningKey())       // 서버 secret key로 토큰 서명, 이거 때문에 나중에 서버가 우리 서버에서 만든 것인지, 조작 있었는지 검증이 가능하다
                .compact();      //지금 까지 설정한 내용을 기반으로 해서, 실제 JWT 문자열로 만든 후 반환 (형태: xxxx.yyyyy.zzz 이런 식)

    }

    // 토큰 파싱
    public Long getUserId(String token) {
        // subject에 저장된 userId를 String → Long으로 변환해서 반환
        return Long.parseLong(parseClaims(token).getSubject());
    }

    // 토큰 유효성 검증
    // 유효하면 true, 만료 or 위변조 등이면 false
    public boolean validateToken(String token) {
        try {
            parseClaims(token); // 파싱 시 만료, 위변조 등이면 예외 발생
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //payload 파싱 (userId를 꺼내서 누가 보낸 요청인지 알기 위해서)
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // Secret Key로 서명 검증
                .build()
                .parseSignedClaims(token)   // 토큰 파싱
                .getPayload();              // payload(Claims) 반환
    }

     // application.yml의 secret 문자열을 SecretKey 객체로 변환
    private SecretKey getSigningKey() {
        // Base64로 인코딩된 secret 문자열을 디코딩해서 SecretKey 객체 생성
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
