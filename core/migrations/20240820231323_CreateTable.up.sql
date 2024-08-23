-- Add up migration script here
CREATE TABLE manga(
  id INTEGER NOT NULL PRIMARY KEY,
  title TEXT NOT NULL,
  url TEXT NOT NULL,
  image TEXT,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE UNIQUE INDEX idx_manga_url ON manga (url);

CREATE TABLE chapter(
  id INTEGER NOT NULL PRIMARY KEY,
  manga INTEGER NOT NULL, -- manga.id
  title TEXT NOT NULL,
  title_number REAL NOT NULL,
  url TEXT NOT NULL,
  is_read INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE INDEX idx_chapter_manga ON chapter (manga);
CREATE UNIQUE INDEX idx_chapter_manga_title ON chapter (manga, title);
