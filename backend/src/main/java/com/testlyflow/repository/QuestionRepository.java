package com.testlyflow.repository;

import com.testlyflow.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByTestIdOrderByNumberAsc(Long testId);
}
