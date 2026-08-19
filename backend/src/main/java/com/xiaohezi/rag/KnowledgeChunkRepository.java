package com.xiaohezi.rag;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunkEntity, String> {}
