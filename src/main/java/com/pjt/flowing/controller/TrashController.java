package com.pjt.flowing.controller;

import com.pjt.flowing.dto.request.AcceptRequestDto;
import com.pjt.flowing.dto.response.ProjectResponseDto;
import com.pjt.flowing.repository.ProjectRepository;
import com.pjt.flowing.service.TrashService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class TrashController {

    private final TrashService trashService;

    @PostMapping("project/trash") // 만든 사람은 휴지통으로 보내고 복구하기, member가 작동시 프로젝트멤버에서 삭제
    public String trashProject(@RequestBody AcceptRequestDto requestDto) {
        return trashService.trashProject(requestDto);
    }

    @GetMapping("project/trash/{userId}") // 휴지통에 있는 프로젝트만 보여주기
    public List<ProjectResponseDto> showTrash(@PathVariable Long userId) {
        return trashService.showTrash(userId);
    }
}