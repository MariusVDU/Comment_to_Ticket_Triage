package com.pulsedesk.triage.repo;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsedesk.triage.domain.CommentAnalysisStatus;
import com.pulsedesk.triage.domain.CommentEntity;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
	Optional<CommentEntity> findFirstByAnalysisStatusOrderByCreatedAtAscIdAsc(CommentAnalysisStatus status);

	Optional<CommentEntity> findFirstByAnalysisStatusAndAnalysisAttemptsLessThanAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
		CommentAnalysisStatus status,
		int analysisAttempts,
		Instant nextAttemptAt
	);
}

