package com.ithwx.personalknowledgebase.practice.domain;

import java.util.List;
import java.util.Optional;

public interface PracticeRepository {

    PracticeSession save(PracticeSession session);

    Optional<PracticeSession> findById(Long id);

    List<PracticeSession> findNeedsReview();
}
