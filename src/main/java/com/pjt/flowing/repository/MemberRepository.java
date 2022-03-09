package com.pjt.flowing.repository;

import com.pjt.flowing.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface MemberRepository extends JpaRepository<Member,Long> {
//    boolean existsByKakaoId(Long kakaoId);
    Optional<Member> findByKakaoId(Long kakaoId);

}