package com.likelion.likelioncrud.post.api;

import com.likelion.likelioncrud.common.response.code.SuccessCode;
import com.likelion.likelioncrud.common.template.ApiResTemplate;
import com.likelion.likelioncrud.post.api.dto.request.PostSaveRequestDto;
import com.likelion.likelioncrud.post.api.dto.request.PostUpdateRequestDto;
import com.likelion.likelioncrud.post.api.dto.response.PostInfoResponseDto;
import com.likelion.likelioncrud.post.application.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
@Tag(name = "POST API", description = "게시글 관리하는 api ")
public class PostController {

    private final PostService postService;

    // 게시물 저장
    @PostMapping()
    @Operation(summary = "게시물 저장", description = "게시물 저장.")
    public ApiResTemplate<Void> postSave(@AuthenticationPrincipal Long userId, @RequestBody @Valid PostSaveRequestDto postSaveRequestDto)
    {
        postService.postSave(userId,postSaveRequestDto);
        return ApiResTemplate.successWithNoContent(SuccessCode.POST_SAVE_SUCCESS);
    }

    // 사용자 id를 기준으로 해당 사용자가 작성한 게시글 목록 조회 (AI파트는 조회도 못하기 때문에, 매개변수 userId 추가)
    @GetMapping("/{memberId}")
    @Operation(summary = "게시물 memberId로 조회", description = "게시물 memberId로 조회")
    public ApiResTemplate<Page<PostInfoResponseDto>> myPostFindAll(@AuthenticationPrincipal Long userId,
                                                                   @PathVariable("memberId") Long memberId,
                                                                   @ParameterObject @PageableDefault(size = 10, sort = "postId", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<PostInfoResponseDto> posts =postService.postFindMember(userId, memberId, pageable);
        return ApiResTemplate.successResponse(SuccessCode.GET_SUCCESS, posts);
    }

    // 게시물 id를 기준으로 사용자가 작성한 게시물 수정
    @PatchMapping("/{postId}")
    @Operation(summary = "게시물 Id로 수정", description = "로그인한 사용자중, BACKEND만 게시글을 수정!")
    public ApiResTemplate<Void> postUpdate(@AuthenticationPrincipal Long userId,
                                           @PathVariable("postId") Long postId,
                                           @RequestBody PostUpdateRequestDto postUpdateRequestDto)
    {
        postService.postUpdate(userId,postId, postUpdateRequestDto);
        return ApiResTemplate.successWithNoContent(SuccessCode.POST_UPDATE_SUCCESS);
    }

    // 게시물 id를 기준으로 사용자가 작성한 게시물 삭제
    @DeleteMapping("/{postId}")
    @Operation(summary = "게시물 삭제", description = "로그인한 사용자 중, BACKEND만 게시글을 삭제!")
    public ApiResTemplate<Void> postDelete(@AuthenticationPrincipal Long userId ,@PathVariable("postId") Long postId) {
        postService.postDelete(userId,postId);
        return ApiResTemplate.successWithNoContent(SuccessCode.POST_DELETE_SUCCESS);
    }
}