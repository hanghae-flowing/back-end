package com.pjt.flowing.repository;

import com.pjt.flowing.model.GapTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GapTableRepository extends JpaRepository<GapTable,Long> {
    List<GapTable> findAllByProject_Id(Long projectId);
}
