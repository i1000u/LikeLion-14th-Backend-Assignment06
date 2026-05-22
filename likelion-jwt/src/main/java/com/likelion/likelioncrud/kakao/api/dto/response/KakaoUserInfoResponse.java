package com.likelion.likelioncrud.kakao.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.likelion.likelioncrud.common.exception.BusinessException;
import com.likelion.likelioncrud.common.response.code.ErrorCode;

import java.util.Properties;

public record KakaoUserInfoResponse(

        Long id,
        @JsonProperty("properties")
        Properties properties,

        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount
) {

    public record Properties(
            String nickname
    ){}

    public record KakaoAccount(
            String email
    ) {}

    public String getNickname() {
        if (properties == null || properties.nickname() == null) {
            return "카카오 사용자";
        }
        return properties().nickname();
    }

    public String getEmail() {
        if (kakaoAccount == null || kakaoAccount.email() == null) {
        throw new BusinessException(ErrorCode.KAKAO_EMAIL_NOT_FOUND_EXCEPTION, ErrorCode.KAKAO_EMAIL_NOT_FOUND_EXCEPTION.getMessage());
    }
        return kakaoAccount().email();

    }
}

