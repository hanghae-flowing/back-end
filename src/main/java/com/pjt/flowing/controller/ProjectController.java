package com.pjt.flowing.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pjt.flowing.dto.*;
import com.pjt.flowing.dto.request.AcceptRequestDto;
import com.pjt.flowing.dto.request.ProjectCreateRequestDto;
import com.pjt.flowing.dto.request.ProjectEditRequestDto;
import com.pjt.flowing.dto.response.ProjectResponseDto;
import com.pjt.flowing.dto.response.MsgResponseDto;
import com.pjt.flowing.model.*;
import com.pjt.flowing.repository.BookmarkRepository;
import com.pjt.flowing.repository.MemberRepository;
import com.pjt.flowing.repository.ProjectMemberRepository;
import com.pjt.flowing.repository.ProjectRepository;
import com.pjt.flowing.security.Authorization;
import com.pjt.flowing.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;

@RequiredArgsConstructor
@RestController
public class ProjectController {
    private final ProjectService projectService;
    private final MemberRepository memberRepository;
    private final Authorization authorization;
    private final ProjectRepository projectRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @PostMapping("/project/detail") // 더보기페이지
    public List<ProjectResponseDto> getProject(@RequestBody AuthorizationDto requestDto) throws JsonProcessingException {
        if(authorization.getKakaoId(requestDto)==0){
            System.out.println("인가x");
        }
        return projectService.getAll(requestDto.getUserId());
    }

    @Transactional
    @PostMapping("/project")    //프로젝트 생성
    public String createProject(@RequestBody ProjectCreateRequestDto projectCreateRequestDto) throws JsonProcessingException {
        return projectService.createProject(projectCreateRequestDto);
    }

    @Transactional
    @PostMapping("/bookmark/{projectId}")   //북마크 생성
    public MsgResponseDto CheckBookmark(@PathVariable Long projectId , @RequestBody AuthorizationDto authorizationDto) {
        System.out.println(projectId);
        boolean check = bookmarkRepository.existsByMember_IdAndProject_Id(authorizationDto.getUserId(), projectId);

        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new IllegalArgumentException("no Project")
        );
        Member member = memberRepository.findById(authorizationDto.getUserId()).orElseThrow(
                () -> new IllegalArgumentException("no Id")
        );
        MsgResponseDto msgResponseDto = new MsgResponseDto();
        if (!check) {
            Bookmark bookmark = new Bookmark(project, member);
            bookmarkRepository.save(bookmark);
            msgResponseDto.setMsg("check");
            return msgResponseDto;
        }
        else {
            bookmarkRepository.deleteByMember_IdAndProject_Id(authorizationDto.getUserId(), projectId);
            msgResponseDto.setMsg("cancel");
            return msgResponseDto;
        }
    }

    @DeleteMapping("/project/{projectId}")    //프로젝트 삭제
    public String deleteProject(@PathVariable Long projectId,@RequestBody AuthorizationDto dto){
        return projectService.deleteproject(projectId, dto);
    }

    @PutMapping("/project/{projectId}")     //프로젝트 수정(파티장만 가능하게 해달랬음)
    public String editProject(@PathVariable Long projectId, ProjectEditRequestDto dto) {
        return projectService.editproject(projectId, dto);
    }

    @GetMapping("/project/{projectId}")   //프로젝트 상세페이지 정보 보내주기
    public String detail(@PathVariable Long projectId){
        return projectService.detail(projectId);
    }

    @PostMapping("/myproject")             //자기가만든 프로젝트 리스트
    public List<ProjectResponseDto> inProject(@RequestBody AuthorizationDto requestDto) {
        return projectService.getAllCreate(requestDto.getUserId());
    }

    @PostMapping("/bookmarked")            //북마크한 프로젝트 리스트
    public List<ProjectResponseDto> getProjectBookmarked(@RequestBody AuthorizationDto requestDto){
        return projectService.getAllBookmarked(requestDto.getUserId());

    }

    @PostMapping("/invitation")  //초대 수락하는 api
    public String accept(@RequestBody AcceptRequestDto acceptRequestDto){
        return projectService.accept(acceptRequestDto);
    }

    @GetMapping("/project/{projectid}/templates")   //모든 템플릿 리스트 불러오기
    public String showTemplates(@PathVariable Long projectid){
        return projectService.showTemplates(projectid);
    }

}
