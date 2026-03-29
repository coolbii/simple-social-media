INSERT INTO users (
  id,
  phone_number,
  user_name,
  email,
  password_hash,
  cover_image_key,
  cover_image_url,
  biography
) VALUES
  (
    1,
    '+886912345678',
    'Brian',
    'brian@example.com',
    '$2a$10$Kr1RgL2pV.Ush5cCeznto.1tL3WuTsyWhFYm8PUKQZVhpSi8SJ.6G',
    'cover-images/brian-cover.png',
    'https://example-bucket.s3.amazonaws.com/cover-images/brian-cover.png',
    'Seed user for the social media scaffold.'
  )
ON DUPLICATE KEY UPDATE user_name = VALUES(user_name);

INSERT INTO posts (
  id,
  user_id,
  content,
  image_key,
  image_url
) VALUES
  (
    1,
    1,
    'Hello from the SQL seed data. This mirrors the in-memory scaffold response.',
    'post-images/sample-post.png',
    'https://example-bucket.s3.amazonaws.com/post-images/sample-post.png'
  )
ON DUPLICATE KEY UPDATE content = VALUES(content);

INSERT INTO comments (
  id,
  post_id,
  user_id,
  content
) VALUES
  (
    1,
    1,
    1,
    'Seed comment for the SSE-ready post detail page.'
  )
ON DUPLICATE KEY UPDATE content = VALUES(content);
