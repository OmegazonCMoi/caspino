INSERT INTO users (username, email, password_hash, is_active)
VALUES
  ('admin', 'admin@example.com', '$2b$10$abcdefghijklmnopqrstuv', TRUE),
  ('johndoe', 'john.doe@example.com', '$2b$10$1234567890123456789012', TRUE),
  ('inactive_user', 'inactive@example.com', '$2b$10$inactivepasswordhash', FALSE);
