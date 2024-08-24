mod db;
mod error;
mod log;
mod sites;

use std::{
    sync::{Arc, LazyLock, Once},
    time::{SystemTime, UNIX_EPOCH},
};

use anyhow::bail;
use db::{ChapterDb, Db, MangaData};
use error::MangaError;
use log::{FFILogLayer, Logger};
use sites::{jmangaorg::Jmangaorg, mangatopjp::MangaTopJp, spoilerplustv::Spoilerplustv, Link, MangaSite};
use tracing_subscriber::{layer::SubscriberExt, Registry};

static _RT: LazyLock<tokio::runtime::Runtime> = LazyLock::new(|| {
    tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .build()
        .expect("failed to initialize tokio runtime")
});

pub(crate) fn rt() -> &'static tokio::runtime::Runtime {
    &_RT
}

static INIT_LOGGER: Once = Once::new();

pub fn init_logger(logger: Arc<dyn Logger>) {
    INIT_LOGGER.call_once(|| {
        let subscriber = Registry::default().with(FFILogLayer(logger));
        tracing::subscriber::set_global_default(subscriber)
            .expect("failed to set global subscriber");
    })
}

#[derive(Debug)]
pub struct Manga {
    db: Db,
}

impl Manga {
    pub fn new(config: Config) -> anyhow::Result<Self> {
        rt().block_on(async move {
            let db = Db::new(config.database_url.clone())?;
            Ok(Self { db })
        })
    }

    pub fn supported_sites(&self) -> Vec<Arc<dyn MangaSite>> {
        vec![
            Arc::new(Spoilerplustv::default()),
            Arc::new(MangaTopJp::default()),
            Arc::new(Jmangaorg::default()),
        ]
    }

    pub async fn migration_available(&self) -> Result<bool, MangaError> {
        let db = self.db.clone();
        rt().spawn(async move { db.migration_available().await })
            .await?
    }

    pub async fn do_migration(&self) -> anyhow::Result<()> {
        let db = self.db.clone();
        rt().spawn(async move { db.do_migration().await }).await?
    }

    pub fn reset_db(&self) -> anyhow::Result<()> {
        self.db.reset()
    }

    pub async fn list_manga(&self) -> anyhow::Result<Vec<MangaData>> {
        let db = self.db.clone();
        rt().spawn(async move { db.list_manga().await }).await?
    }

    pub async fn open_manga(&self, link: Link) -> anyhow::Result<MangaData> {
        let db = self.db.clone();
        rt().spawn(async move {
            let link = link;
            let manga = match db.find_manga_by_url(&link.url).await? {
                Some(mut manga) => {
                    manga.updated_at =
                        SystemTime::now().duration_since(UNIX_EPOCH)?.as_secs() as i64;
                    db.update_manga_time(manga.id, manga.updated_at).await?;
                    manga
                }
                None => db.create_manga(link.text, link.url, link.image).await?,
            };
            Ok(manga)
        })
        .await?
    }

    pub async fn open_manga_with_id(&self, id: i64) -> anyhow::Result<()> {
        let db = self.db.clone();
        rt().spawn(async move {
            let ts = SystemTime::now().duration_since(UNIX_EPOCH)?.as_secs() as i64;
            db.update_manga_time(id, ts).await?;
            Ok(())
        }).await?
    }

    pub async fn get_manga(&self, id: i64) -> anyhow::Result<Option<MangaData>> {
        let db = self.db.clone();
        rt().spawn(async move { db.find_manga(id).await }).await?
    }

    pub async fn delete_manga(&self, id: i64) -> anyhow::Result<()> {
        let db = self.db.clone();
        rt().spawn(async move {
            db.delete_manga(id).await
        }).await?
    }

    pub async fn get_chapter(&self, id: i64) -> anyhow::Result<Option<ChapterDb>> {
        let db = self.db.clone();
        rt().spawn(async move { db.find_chapter(id).await }).await?
    }

    pub async fn mark_chapter_read(&self, id: i64, is_read: bool) -> anyhow::Result<()> {
        let db = self.db.clone();
        rt().spawn(async move { db.mark_chapter_read(id, is_read).await })
            .await?
    }

    pub async fn get_chapters(&self, url: String) -> anyhow::Result<Vec<ChapterDb>> {
        let db = self.db.clone();
        let sites = self.supported_sites();
        rt().spawn(async move {
            let url = url;
            let site = match sites.iter().find(|s| s.can_handle_chapters(&url)) {
                Some(site) => site,
                None => bail!("Couldn't find site handler for {}", url),
            };

            let manga = match db.find_manga_by_url(&url).await? {
                Some(manga) => manga,
                None => bail!("the url isn't in database yet: {}", url),
            };

            let chapters = site.chapters(&url).await?;

            // TODO: insert to db
            for chapter in chapters.into_iter() {
                let _ = match db
                    .find_chapter_by_title(manga.id, chapter.text.clone())
                    .await?
                {
                    Some(in_db) => {
                        db.update_chapter(manga.id, chapter.text, chapter.url)
                            .await?;
                        in_db
                    }
                    None => {
                        db.create_chapter(manga.id, chapter.text, chapter.url)
                            .await?
                    }
                };
            }

            let chapters = db.get_chapters(manga.id).await?;
            Ok(chapters)
        })
        .await?
    }

    pub async fn get_chapters_cache(&self, url: String) -> anyhow::Result<Vec<ChapterDb>> {
        let db = self.db.clone();
        rt().spawn(async move {
            let url = url;

            let manga = match db.find_manga_by_url(&url).await? {
                Some(m) => m,
                None => return Ok(vec![]),
            };

            let chapters = db.get_chapters(manga.id).await?;
            Ok(chapters)
        })
        .await?
    }

    pub fn get_site(&self, url: String) -> anyhow::Result<Option<Arc<dyn MangaSite>>> {
        let site = self
            .supported_sites()
            .into_iter()
            .find(|s| s.can_handle_images(&url));
        Ok(site)
    }

    pub async fn get_images(&self, url: String) -> anyhow::Result<Vec<String>> {
        let sites = self.supported_sites();
        rt().spawn(async move {
            let url = url;
            let site = match sites.iter().find(|s| s.can_handle_images(&url)) {
                Some(site) => site,
                None => bail!("Couldn't find site handler for {}", url),
            };

            let images = site.images(&url).await?;
            Ok(images)
        })
        .await?
    }
}

#[derive(Debug)]
pub struct Config {
    pub database_url: String,
}

impl Default for Config {
    fn default() -> Self {
        Self {
            database_url: "sqlite://database.db".to_string(),
        }
    }
}

uniffi::include_scaffolding!("manga");
