package com.pjt.flowing.dto.response.invite;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InviteResponseDto {

    private Long invitingId;
    private String inviting;
    private String projectName;

    public InviteResponseDto(Long invitingId, String inviting, String projectName){
        this.invitingId = invitingId;
        this.inviting=inviting;
        this.projectName = projectName;
    }
}