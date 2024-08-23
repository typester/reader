use std::{
    collections::HashMap,
    fs::File,
    path::Path,
    time::{SystemTime, UNIX_EPOCH},
};

use anyhow::bail;
use lazy_static::lazy_static;
use regex::Regex;
use sqlx::{migrate::Migrate, Connection, SqliteConnection, SqlitePool};

use crate::error::MangaError;

#[derive(Debug, Clone)]
pub struct Db {
    database_url: String,
    pool: SqlitePool,
}

impl Db {
    pub fn new(database_url: String) -> anyhow::Result<Self> {
        let pool = SqlitePool::connect_lazy(&database_url)?;
        Ok(Self { database_url, pool })
    }

    pub async fn migration_available(&self) -> Result<bool, MangaError> {
        let migrator = sqlx::migrate!();
        // Seems like the SqlitePool doesn't implement Migrate trait
        let mut conn = SqliteConnection::connect(&self.database_url).await?;

        conn.ensure_migrations_table().await?;

        let applied_migrations: HashMap<_, _> = conn
            .list_applied_migrations()
            .await?
            .into_iter()
            .map(|m| (m.version, m))
            .collect();

        let has_mismatched_migration = migrator
            .iter()
            .filter(|m| !m.migration_type.is_down_migration())
            .find(|m| {
                if let Some(applied) = applied_migrations.get(&m.version) {
                    m.checksum != applied.checksum
                } else {
                    false
                }
            });
        if has_mismatched_migration.is_some() {
            return Err(MangaError::MigrateError {
                msg: "mismatched migration is found".into(),
            });
        }

        let has_unapplied_migration = match migrator
            .iter()
            .filter(|m| !m.migration_type.is_down_migration())
            .find(|m| !applied_migrations.contains_key(&m.version))
        {
            Some(_) => true,
            None => false,
        };

        Ok(has_unapplied_migration)
    }

    pub async fn do_migration(&self) -> anyhow::Result<()> {
        let pool = self.pool.clone();
        let migrator = sqlx::migrate!();
        Ok(migrator.run(&pool).await?)
    }

    pub fn reset(&self) -> anyhow::Result<()> {
        let path = Path::new(&self.database_url["sqlite://".len()..]);
        let f = File::create(path)?;
        f.set_len(0)?;
        Ok(())
    }

    pub async fn list_manga(&self) -> anyhow::Result<Vec<MangaDb>> {
        let list: Vec<MangaDb> = sqlx::query_as("SELECT * FROM manga ORDER BY updated_at DESC")
            .fetch_all(&self.pool)
            .await?;
        Ok(list)
    }

    pub async fn create_manga(
        &self,
        title: String,
        url: String,
        image: Option<String>,
    ) -> anyhow::Result<MangaDb> {
        let ts = SystemTime::now().duration_since(UNIX_EPOCH)?.as_secs();

        let _ = sqlx::query(
            "INSERT INTO manga (title, url, image, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
        )
        .bind(&title)
        .bind(&url)
        .bind(&image)
        .bind(ts as i64)
        .bind(ts as i64)
        .execute(&self.pool)
        .await?;

        let manga: MangaDb = sqlx::query_as("SELECT * FROM manga WHERE url = ?")
            .bind(&url)
            .fetch_one(&self.pool)
            .await?;

        Ok(manga)
    }

    pub async fn find_manga(&self, id: i64) -> anyhow::Result<Option<MangaDb>> {
        let manga: Option<MangaDb> = sqlx::query_as("SELECT * FROM manga WHERE id = ?")
            .bind(id)
            .fetch_optional(&self.pool)
            .await?;
        Ok(manga)
    }

    pub async fn find_manga_by_url(&self, url: &str) -> anyhow::Result<Option<MangaDb>> {
        let manga: Option<MangaDb> = sqlx::query_as("SELECT * FROM manga WHERE url = ?")
            .bind(url)
            .fetch_optional(&self.pool)
            .await?;
        Ok(manga)
    }

    pub async fn update_manga_time(&self, id: i64, ts: i64) -> anyhow::Result<()> {
        let _ = sqlx::query("UPDATE manga SET updated_at = ? WHERE id = ?")
            .bind(ts)
            .bind(id)
            .execute(&self.pool)
            .await?;
        Ok(())
    }

    pub async fn find_chapter(&self, chapter_id: i64) -> anyhow::Result<Option<ChapterDb>> {
        let chapter: Option<ChapterDb> = sqlx::query_as("SELECT * FROM chapter WHERE id = ?")
            .bind(chapter_id)
            .fetch_optional(&self.pool)
            .await?;
        Ok(chapter)
    }

    pub async fn find_chapter_by_title(
        &self,
        manga_id: i64,
        title: String,
    ) -> anyhow::Result<Option<ChapterDb>> {
        let chapter: Option<ChapterDb> =
            sqlx::query_as("SELECT * FROM chapter WHERE manga = ? AND title = ?")
                .bind(manga_id)
                .bind(&title)
                .fetch_optional(&self.pool)
                .await?;
        Ok(chapter)
    }

    pub async fn create_chapter(
        &self,
        manga_id: i64,
        title: String,
        url: String,
    ) -> anyhow::Result<ChapterDb> {
        lazy_static! {
            static ref RE: Regex = Regex::new(r"([\d.]+)").unwrap();
        }

        let title_num = if let Some(m) = RE.captures(&title) {
            m.get(1).unwrap().as_str().parse::<f64>()?
        } else {
            bail!("failed to extract number from chapter title");
        };

        let ts = SystemTime::now().duration_since(UNIX_EPOCH)?.as_secs();
        let _ = sqlx::query(
            "INSERT INTO chapter (manga, title, title_number, url, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
        )
        .bind(manga_id)
        .bind(&title)
        .bind(title_num)
        .bind(&url)
        .bind(ts as i64)
        .bind(ts as i64)
        .execute(&self.pool)
        .await?;

        let chapter: ChapterDb =
            sqlx::query_as("SELECT * FROM chapter WHERE manga = ? AND title = ?")
                .bind(manga_id)
                .bind(&title)
                .fetch_one(&self.pool)
                .await?;

        Ok(chapter)
    }

    pub async fn update_chapter(
        &self,
        manga_id: i64,
        title: String,
        url: String,
    ) -> anyhow::Result<()> {
        let ts = SystemTime::now().duration_since(UNIX_EPOCH)?.as_secs();
        let _ =
            sqlx::query("UPDATE chapter SET url = ?, updated_at = ? WHERE manga = ? AND title = ?")
                .bind(&url)
                .bind(ts as i64)
                .bind(manga_id)
                .bind(&title)
                .execute(&self.pool)
                .await?;
        Ok(())
    }

    pub async fn mark_chapter_read(&self, chapter_id: i64, is_read: bool) -> anyhow::Result<()> {
        let ts = SystemTime::now().duration_since(UNIX_EPOCH)?.as_secs();
        let _ = sqlx::query("UPDATE chapter SET is_read = ?, updated_at = ? WHERE id = ?")
            .bind(if is_read { 1 } else { 0 })
            .bind(ts as i64)
            .bind(chapter_id)
            .execute(&self.pool)
            .await?;
        Ok(())
    }

    pub async fn get_chapters(&self, manga_id: i64) -> anyhow::Result<Vec<ChapterDb>> {
        let chapters: Vec<ChapterDb> =
            sqlx::query_as("SELECT * FROM chapter WHERE manga = ? ORDER BY title_number DESC")
                .bind(manga_id)
                .fetch_all(&self.pool)
                .await?;
        Ok(chapters)
    }
}

#[derive(sqlx::FromRow)]
pub struct MangaDb {
    pub id: i64,
    pub title: String,
    pub url: String,
    pub image: Option<String>,
    pub created_at: i64,
    pub updated_at: i64,
}

#[derive(sqlx::FromRow)]
pub struct ChapterDb {
    pub id: i64,
    pub manga: i64,
    pub title: String,
    pub title_number: f64,
    pub url: String,
    pub is_read: i64,
    pub created_at: i64,
    pub updated_at: i64,
}
