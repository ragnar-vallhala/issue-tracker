package com.its.project.repository;

import com.its.project.entity.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Integer> {

    List<Project> findByProjectOwnerId(Integer projectOwnerId);

    Optional<Project> findByProjectName(String projectName);

    boolean existsByProjectName(String projectName);
}
