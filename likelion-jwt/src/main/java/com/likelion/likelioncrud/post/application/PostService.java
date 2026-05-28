package com.likelion.likelioncrud.post.application;

import com.likelion.likelioncrud.common.exception.BusinessException;
import com.likelion.likelioncrud.common.response.code.ErrorCode;
import com.likelion.likelioncrud.member.domain.Member;
import com.likelion.likelioncrud.member.domain.Part;
import com.likelion.likelioncrud.member.domain.repository.MemberRepository;
import com.likelion.likelioncrud.post.api.dto.request.PostSaveRequestDto;
import com.likelion.likelioncrud.post.api.dto.request.PostUpdateRequestDto;
import com.likelion.likelioncrud.post.api.dto.response.PostInfoResponseDto;
import com.likelion.likelioncrud.post.api.dto.response.PostListResponseDto;
import com.likelion.likelioncrud.post.domain.Post;
import com.likelion.likelioncrud.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    // 게시물 저장
    @Transactional
    public void postSave(Long userId,PostSaveRequestDto postSaveRequestDto) {
        Member member = findMemberById(userId);

        checkBackendPermission(member);

        Post post = Post.builder()
                .title(postSaveRequestDto.title())
                .contents(postSaveRequestDto.contents())
                .member(member)
                .build();

        postRepository.save(post);
    }

    // 특정 작성자가 작성한 게시글 목록을 조회 - AI파트일시 조회 기능 제거함
    public Page<PostInfoResponseDto> postFindMember(Long userId,Long memberId, Pageable pageable) {
        Member member = findMemberById(userId); //jwt 토큰 userId를 받아온다.
        checkAIPermission(member); // AI파트인지 검사한다. AI파트일시, 오류 발생

        Member targetMember = findMemberById(memberId); //문제 없을시, 조회할 게시글 id인 memberId를 통해 객체 생성 후 반환한다.
        Page<Post> posts = postRepository.findByMember(targetMember, pageable);
        return posts.map(PostInfoResponseDto::from);
    }

    // 게시물 수정
    @Transactional
    public void postUpdate(Long userId,Long postId, PostUpdateRequestDto postUpdateRequestDto)
    {
        Member member = findMemberById(userId);
        checkBackendPermission(member);
        Post post = findPostById(postId);

        checkPostOwner(member,post);

        post.update(postUpdateRequestDto);
    }

    // 게시물 삭제
    @Transactional
    public void postDelete(Long userId, Long postId) {
        Member member = findMemberById(userId);

        checkBackendPermission(member);

        Post post = findPostById(postId);
        checkPostOwner(member,post);
        postRepository.delete(post);
    }

    //추가
    private Member findMemberById(Long memberId) {
        return  memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MEMBER_NOT_FOUND_EXCEPTION,
                        ErrorCode.MEMBER_NOT_FOUND_EXCEPTION.getMessage() + memberId));
    }

    private Post findPostById(Long postId) {
        return  postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.POST_NOT_FOUND_EXCEPTION,
                        ErrorCode.POST_NOT_FOUND_EXCEPTION.getMessage() + postId));
    }

    private void checkBackendPermission(Member member) {
        if (member.getPart() != Part.BACKEND) {
            throw new BusinessException(ErrorCode.FORBIDDEN_EXCEPTION, ErrorCode.FORBIDDEN_EXCEPTION.getMessage());
        }
    }

    //AI파트일시, FORBIDDEN_EXCEPTION을 발생시키는 메소드 (AI파트일 시, 조회불가로 만들기 위한 로직)
    private void checkAIPermission(Member member) {
        if (member.getPart() == Part.AI) {
            throw new BusinessException(ErrorCode.FORBIDDEN_EXCEPTION, ErrorCode.FORBIDDEN_EXCEPTION.getMessage());
        }
    }

    //BACKEND파트가 수정,삭제를 하기 위해, 게시글 작성자이면서, 동시에 BACKEND파트인지 검사하는 로직
    private void checkPostOwner(Member member, Post post) {
        //이미 수정,삭제 메소드에서 백엔드인지 검사했기때문에 또 검사 안해도됨.
        //글을 쓴 사용자의 ID와 로그인한 사용자의 jwt 토큰 내부 id를 가져와, .equals를 통해 비교(동등성)
        if (!post.getMember().getMemberId().equals(member.getMemberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_EXCEPTION, ErrorCode.FORBIDDEN_EXCEPTION.getMessage());
        }
    }

}