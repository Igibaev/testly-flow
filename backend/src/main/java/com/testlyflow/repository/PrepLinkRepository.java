package com.testlyflow.repository;

import com.testlyflow.entity.PrepLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrepLinkRepository extends JpaRepository<PrepLink, Long> {

    List<PrepLink> findByTestIdOrderBySortOrderAsc(Long testId);

    void deleteByTestId(Long testId);
}
