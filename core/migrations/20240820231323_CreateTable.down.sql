-- Add down migration script here
DROP INDEX idx_chapter_manga_title;
DROP INDEX idx_chapter_manga;
DROP TABLE chapter;
DROP INDEX idx_manga_url;
DROP TABLE manga;
