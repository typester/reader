use std::collections::HashMap;

use async_trait::async_trait;

pub mod jmangaorg;
pub mod mangatopjp;
pub mod spoilerplustv;
pub mod rawkuronet;

#[derive(Debug)]
pub struct Link {
    pub text: String,
    pub url: String,
    pub image: Option<String>,
}

#[async_trait]
pub trait MangaSite: Send + Sync {
    fn name(&self) -> String;

    async fn search(&self, text: String) -> anyhow::Result<Vec<Link>>;

    fn can_handle_chapters(&self, _url: &str) -> bool {
        false
    }
    async fn chapters(&self, _url: &str) -> anyhow::Result<Vec<Link>> {
        Ok(vec![])
    }

    fn can_handle_images(&self, _url: &str) -> bool {
        false
    }
    async fn images(&self, _url: &str) -> anyhow::Result<Vec<String>> {
        Ok(vec![])
    }

    fn request_headers(&self) -> HashMap<String, String> {
        HashMap::new()
    }
}
