package com.agentreview.analysis;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangedFileRepository extends JpaRepository<ChangedFile, Long> {

	List<ChangedFile> findBySessionIdOrderByFilePathAsc(Long sessionId);

	void deleteBySessionId(Long sessionId);
}
