package com.likelion.likelioncrud.post.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PostSaveRequestDto(

        //Long memberId, - part별 권한을 다르게 주기위해, memberId 삭제 처리

        @NotBlank(message = "제목을 필수로 입력해야 합니다.")
        String title,

        @NotBlank(message = "내용을 필수로 입력해야 합니다.")
        String contents
) {
}
