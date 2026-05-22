package com.likelion.likelioncrud.kakao.api;

import com.likelion.likelioncrud.auth.api.dto.response.LoginResponseDto;
import com.likelion.likelioncrud.common.response.code.SuccessCode;
import com.likelion.likelioncrud.common.template.ApiResTemplate;
import com.likelion.likelioncrud.kakao.application.KakaoOauthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/kakao")
@Tag(name="카카오 로그인 API", description = "카카오 소셜 로그인 API")
public class KakaoOauthController {
    private final KakaoOauthService kakaoOauthService;

    @GetMapping("/callback")
    public ApiResTemplate<LoginResponseDto> kakaoCallback(@RequestParam("code") String code){
        LoginResponseDto response = kakaoOauthService.kakaoLogin(code);
        return ApiResTemplate.successResponse(SuccessCode.LOGIN_SUCCESS, response);
    }
}
