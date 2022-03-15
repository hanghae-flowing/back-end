package com.pjt.flowing.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@NoArgsConstructor
@Entity
public class ProjectMember {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name="user_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name="project_id")
    private Project project;
}
