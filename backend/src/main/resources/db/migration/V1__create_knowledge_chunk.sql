CREATE TABLE knowledge_chunk (
  id VARCHAR(80) PRIMARY KEY,
  document_id VARCHAR(80) NOT NULL,
  title VARCHAR(200) NOT NULL,
  section_name VARCHAR(300) NOT NULL,
  content LONGTEXT NOT NULL,
  embedding_json LONGTEXT NULL,
  embedding_model VARCHAR(80) NOT NULL,
  chunk_index INT NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_document_chunk ON knowledge_chunk (document_id, chunk_index);
