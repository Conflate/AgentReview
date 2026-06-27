package com.agentreview.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryProfileRepository extends JpaRepository<RepositoryProfile, Long> {

	Optional<RepositoryProfile> findFirstByRepoNameIgnoreCase(String repoName);
}
