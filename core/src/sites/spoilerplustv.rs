use std::collections::HashMap;

use anyhow::anyhow;
use async_trait::async_trait;
use lazy_static::lazy_static;
use regex::Regex;
use reqwest::header::HeaderMap;
use scraper::{Html, Selector};

use crate::rt;

use super::{Link, MangaSite};

#[derive(Debug)]
pub struct Spoilerplustv {
    client: reqwest::Client,
}

impl Default for Spoilerplustv {
    fn default() -> Self {
        let mut headers = HeaderMap::new();
        headers.insert("Referer", "https://spoilerplus.tv/".parse().unwrap());
        let client = reqwest::ClientBuilder::new()
            .user_agent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36")
            .default_headers(headers)
            .build().expect("failed to initialize http client");
        Self { client }
    }
}

#[async_trait]
impl MangaSite for Spoilerplustv {
    fn name(&self) -> String {
        "spoilerplus.tv".into()
    }

    fn request_headers(&self) -> HashMap<String, String> {
        let mut h = HashMap::new();
        h.insert("Referer".to_string(), "https://spoilerplus.tv/".to_string());
        h
    }

    fn can_handle_chapters(&self, url: &str) -> bool {
        lazy_static! {
            static ref RE: Regex = Regex::new(r"^https://spoilerplus\.tv/[^/]+/$").unwrap();
        }
        RE.is_match(url)
    }

    async fn chapters(&self, url: &str) -> anyhow::Result<Vec<Link>> {
        let res = self.client.get(url).send().await?;
        let res = res.text().await?;

        let doc = Html::parse_document(&res);
        let selector = Selector::parse(".list-chapter .chapter > a").unwrap();

        let results: Vec<Link> = doc
            .select(&selector)
            .filter_map(|e| {
                let text = e.text().next();
                let link = e.attr("href");

                if let (Some(text), Some(link)) = (text, link) {
                    Some(Link {
                        text: text.trim().to_string(),
                        url: format!("https://spoilerplus.tv{}", link),
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
            static ref RE: Regex = Regex::new(r"^https://spoilerplus\.tv/[^/]+/[^/]+/$").unwrap();
        }
        RE.is_match(url)
    }

    async fn images(&self, url: &str) -> anyhow::Result<Vec<String>> {
        let res = self.client.get(url).send().await?;
        let res = res.text().await?;

        let doc = Html::parse_document(&res);
        let selector = Selector::parse("#post-comic .ct").unwrap();

        let results: Vec<String> = doc
            .select(&selector)
            .filter_map(|e| e.attr("data-z"))
            .map(|t| format!("https://cdn1.mangarawspoiler.co{}", t))
            .collect();
        Ok(results)
    }

    async fn search(&self, text: String) -> anyhow::Result<Vec<Link>> {
        let client = self.client.clone();
        rt().spawn(async move {
            tracing::info!("search spoilerplus");
            let mut query = HashMap::new();
            query.insert("s", text);

            let res = client
                .get("https://spoilerplus.tv/")
                .query(&query)
                .send()
                .await?;
            let res = res.text().await?;

            let doc = Html::parse_document(&res);
            let selector =
                Selector::parse("article.item").map_err(|e| anyhow!(format!("error: {:?}", e)))?;
            let caption = Selector::parse("figcaption h3 a")
                .map_err(|e| anyhow!(format!("error: {:?}", e)))?;
            let image =
                Selector::parse(".image img").map_err(|e| anyhow!(format!("error: {:?}", e)))?;

            let results: Vec<Link> = doc
                .select(&selector)
                .filter_map(|element| {
                    let caption = element.select(&caption).next();
                    let image = element.select(&image).next();

                    if let Some(caption) = caption {
                        Some((caption, image))
                    } else {
                        None
                    }
                })
                .filter_map(|(caption, image)| {
                    let title = caption.text().next();
                    let link_path = caption.attr("href");

                    if title.is_some() && link_path.is_some() {
                        let mut link = Link {
                            text: title.unwrap().to_string(),
                            url: format!("https://spoilerplus.tv{}", link_path.unwrap()),
                            image: None,
                        };
                        if let Some(image) = image {
                            if let Some(src) = image.attr("data-src") {
                                link.image = Some(format!("https://spoilerplus.tv{}", src));
                            }
                        }
                        Some(link)
                    } else {
                        None
                    }
                })
                .collect();

            Ok(results)
        })
        .await?
    }
}

#[cfg(test)]
mod tests {
    use std::sync::Arc;

    use crate::sites::MangaSite;

    #[tokio::test]
    async fn test_search() {
        let s: Arc<dyn MangaSite> = Arc::new(super::Spoilerplustv::default());
        let res = s.search("one".into()).await;

        assert!(res.is_ok());
        let res = res.unwrap();

        assert!(res.len() > 0);
        println!("res[0]: {:?}", res[0]);
    }
}
