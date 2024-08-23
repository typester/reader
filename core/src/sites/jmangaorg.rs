use std::collections::HashMap;

use anyhow::{anyhow, bail};
use async_trait::async_trait;
use lazy_static::lazy_static;
use regex::Regex;
use scraper::{Html, Selector};

use crate::rt;

use super::{Link, MangaSite};

#[derive(Debug)]
pub struct Jmangaorg {
    client: reqwest::Client,
}

impl Default for Jmangaorg {
    fn default() -> Self {
        let client = reqwest::ClientBuilder::new()
            .user_agent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36")
            .build().expect("failed to initialize http client for jmanga");
        Self { client }
    }
}

#[async_trait]
impl MangaSite for Jmangaorg {
    fn name(&self) -> String {
        "jmanga.org".to_string()
    }

    async fn search(&self, text: String) -> anyhow::Result<Vec<Link>> {
        let client = self.client.clone();
        rt().spawn(async move {
            let mut query = HashMap::new();
            query.insert("q", text);

            let res = client
                .get("https://jmanga.org/")
                .query(&query)
                .send()
                .await?;
            let res = res.text().await?;

            let doc = Html::parse_document(&res);

            let container_sel =
                Selector::parse(".manga_list-sbs .item").map_err(|e| anyhow!("error: {:?}", e))?;
            let title_sel =
                Selector::parse(".manga-name a").map_err(|e| anyhow!("error: {:?}", e))?;
            let image_sel =
                Selector::parse(".manga-poster img").map_err(|e| anyhow!("error: {:?}", e))?;

            let results: Vec<Link> = doc
                .select(&container_sel)
                .filter_map(|container| {
                    let title_elem = container.select(&title_sel).next();
                    let image_elem = container.select(&image_sel).next();

                    if let Some(title_elem) = title_elem {
                        Some((title_elem, image_elem))
                    } else {
                        None
                    }
                })
                .filter_map(|(title_elem, image_elem)| {
                    let title = title_elem.text().next();
                    let link = title_elem.attr("href");
                    let image = match image_elem {
                        Some(i) => i.attr("data-src"),
                        None => None,
                    };

                    if let (Some(title), Some(link)) = (title, link) {
                        Some(Link {
                            text: title.to_string(),
                            url: link.to_string(),
                            image: match image {
                                Some(i) => Some(i.to_string()),
                                None => None,
                            },
                        })
                    } else {
                        None
                    }
                })
                .collect();

            Ok(results)
        })
        .await?
    }

    fn can_handle_chapters(&self, url: &str) -> bool {
        lazy_static! {
            static ref RE: Regex = Regex::new(r"^https://jmanga\.org/read/[^/]+/$").unwrap();
        }
        RE.is_match(url)
    }

    async fn chapters(&self, url: &str) -> anyhow::Result<Vec<Link>> {
        let res = self.client.get(url).send().await?;
        let res = res.text().await?;

        let doc = Html::parse_document(&res);
        let sel = Selector::parse("#list-chapter .chapter-item").unwrap();
        let link_sel = Selector::parse("a.item-link").unwrap();

        let results: Vec<Link> = doc
            .select(&sel)
            .filter_map(|elem| {
                let id = elem.attr("data-id");
                let title = match elem.select(&link_sel).next() {
                    Some(e) => e.attr("title"),
                    None => None,
                };

                if let (Some(id), Some(title)) = (id, title) {
                    Some(Link {
                        text: title.to_string(),
                        url: format!("https://jmanga.org/json/chapter?mode=vertical&id={}", id),
                        image: None,
                    })
                } else {
                    None
                }
            })
            .collect();

        Ok(results)
    }

    fn can_handle_images(&self, url: &str) -> bool {
        lazy_static! {
            static ref RE: Regex =
                Regex::new(r"^https://jmanga\.org/json/chapter\?mode=vertical&id=\d+$").unwrap();
        }
        RE.is_match(url)
    }

    async fn images(&self, url: &str) -> anyhow::Result<Vec<String>> {
        let res = self.client.get(url).send().await?;
        let res = res.text().await?;

        let json: serde_json::Value = serde_json::from_str(&res)?;
        let res = match json.get("html").and_then(|v| v.as_str()) {
            Some(s) => s,
            None => bail!("invalid json"),
        };

        let doc = Html::parse_fragment(res);
        let sel = Selector::parse("img").unwrap();

        let results: Vec<String> = doc
            .select(&sel)
            .filter_map(|img| match img.attr("data-src") {
                Some(src) => Some(src.to_string()),
                None => None,
            })
            .collect();
        Ok(results)
    }
}

#[cfg(test)]
mod tests {
    use std::sync::Arc;

    use crate::sites::MangaSite;

    #[tokio::test]
    async fn test_jmanga_search() {
        let s: Arc<dyn MangaSite> = Arc::new(super::Jmangaorg::default());
        let res = s.search("異世界".into()).await;

        assert!(res.is_ok());
        let res = res.unwrap();
        assert!(res.len() > 0);

        println!("results[0]: {:?}", res[0]);
    }

    #[tokio::test]
    async fn test_jmanga_chapters() {
        let s: Arc<dyn MangaSite> = Arc::new(super::Jmangaorg::default());
        let res = s.search("ワンピース".into()).await;

        assert!(res.is_ok());
        let res = res.unwrap();
        assert!(res.len() > 0);
        println!("results[0]: {:?}", res[0]);

        let chapters = s.chapters(&res[0].url).await;
        assert!(chapters.is_ok());
        let chapters = chapters.unwrap();
        assert!(chapters.len() > 0);

        println!("find chapters: {}", chapters.len());
        println!("chapters[0]: {:?}", chapters[0]);
    }

    #[tokio::test]
    async fn test_jmanga_images() {
        let s: Arc<dyn MangaSite> = Arc::new(super::Jmangaorg::default());
        let res = s.search("ワンピース".into()).await;

        assert!(res.is_ok());
        let res = res.unwrap();
        assert!(res.len() > 0);
        println!("results[0]: {:?}", res[0]);

        let chapters = s.chapters(&res[0].url).await;
        assert!(chapters.is_ok());
        let chapters = chapters.unwrap();
        assert!(chapters.len() > 0);

        println!("find chapters: {}", chapters.len());
        println!("chapters[0]: {:?}", chapters[0]);

        let images = s.images(&chapters[0].url).await;
        assert!(images.is_ok());
        let images = images.unwrap();
        assert!(images.len() > 0);

        println!("find images: {}", images.len());
        println!("images: {:?}", images);
    }
}
