use std::collections::HashMap;

use async_trait::async_trait;
use lazy_static::lazy_static;
use regex::Regex;
use scraper::{Html, Selector};

use crate::rt;

use super::{Link, MangaSite};

#[derive(Debug)]
pub struct MangaTopJp {
    client: reqwest::Client,
}

impl Default for MangaTopJp {
    fn default() -> Self {
        let client = reqwest::ClientBuilder::new()
            .user_agent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36")
            .build().expect("failed to initialize http client for mangatopjp");
        Self { client }
    }
}

#[async_trait]
impl MangaSite for MangaTopJp {
    fn name(&self) -> String {
        "mangatopjp.com".to_string()
    }

    async fn search(&self, text: String) -> anyhow::Result<Vec<Link>> {
        let client = self.client.clone();
        rt().spawn(async move {
            let mut query = HashMap::new();
            query.insert("q", text);

            let res = client
                .get("https://mangatopjp.com/search/")
                .query(&query)
                .send()
                .await?;
            let res = res.text().await?;

            let doc = Html::parse_document(&res);
            let sel = Selector::parse(".list-manga .it-left a").unwrap();
            let img_sel = Selector::parse("img").unwrap();

            let results: Vec<Link> = doc
                .select(&sel)
                .filter_map(|elem| {
                    let title = elem.attr("title");
                    let link = elem.attr("href");

                    let img = match elem.select(&img_sel).next() {
                        Some(img) => img.attr("src"),
                        None => None,
                    };

                    if let (Some(title), Some(link)) = (title, link) {
                        Some(Link {
                            text: title.to_string(),
                            url: format!("https://mangatopjp.com{}", link),
                            image: match img {
                                Some(img) => Some(img.to_string()),
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
            static ref RE: Regex = Regex::new(r"^https://mangatopjp\.com/manga/[^/]+/$").unwrap();
        }
        RE.is_match(url)
    }

    async fn chapters(&self, url: &str) -> anyhow::Result<Vec<Link>> {
        let res = self.client.get(url).send().await?;
        let res = res.text().await?;

        let doc = Html::parse_document(&res);
        let sel = Selector::parse(".chapter-item a").unwrap();
        let title_sel = Selector::parse(".ct-name").unwrap();

        let results: Vec<Link> = doc
            .select(&sel)
            .filter_map(|elem| {
                let title = elem.select(&title_sel).next().and_then(|t| t.text().next());
                let link = elem.attr("href");

                if let (Some(title), Some(link)) = (title, link) {
                    Some(Link {
                        text: title.to_string(),
                        url: format!("https://mangatopjp.com{}", link),
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
                Regex::new(r"^https://mangatopjp\.com/manga/[^/]+/[^/]+/$").unwrap();
        }
        RE.is_match(url)
    }

    async fn images(&self, url: &str) -> anyhow::Result<Vec<String>> {
        let res = self.client.get(url).send().await?;
        let res = res.text().await?;

        let doc = Html::parse_document(&res);
        let sel = Selector::parse(".chapter-content img").unwrap();

        let results: Vec<String> = doc
            .select(&sel)
            .filter_map(|img| img.attr("data-src").and_then(|s| Some(s.to_string())))
            .collect();

        Ok(results)
    }
}

#[cfg(test)]
mod tests {
    use std::sync::Arc;

    use crate::sites::MangaSite;

    #[tokio::test]
    async fn test_mangatopjp_search() {
        let s: Arc<dyn MangaSite> = Arc::new(super::MangaTopJp::default());
        let res = s.search("異世界".into()).await;

        assert!(res.is_ok());
        let res = res.unwrap();
        assert!(res.len() > 0);
        println!("result[0]: {:?}", res[0]);
    }

    #[tokio::test]
    async fn test_mangatopjp_chapters() {
        let s: Arc<dyn MangaSite> = Arc::new(super::MangaTopJp::default());
        let res = s.search("異世界".into()).await;

        assert!(res.is_ok());
        let res = res.unwrap();
        assert!(res.len() > 0);
        println!("result[0]: {:?}", res[0]);

        let res = s.chapters(&res[0].url).await;
        assert!(res.is_ok());
        let res = res.unwrap();
        assert!(res.len() > 0);
        println!("chapters[0]: {:?}", res[0]);
    }

    #[tokio::test]
    async fn test_mangatopjp_images() {
        let s: Arc<dyn MangaSite> = Arc::new(super::MangaTopJp::default());
        let res = s.search("異世界".into()).await;

        assert!(res.is_ok());
        let res = res.unwrap();
        assert!(res.len() > 0);
        println!("result[0]: {:?}", res[0]);

        let res = s.chapters(&res[0].url).await;
        assert!(res.is_ok());
        let res = res.unwrap();
        assert!(res.len() > 0);
        println!("chapters[0]: {:?}", res[0]);

        let res = s.images(&res[0].url).await;
        assert!(res.is_ok());
        let res = res.unwrap();
        assert!(res.len() > 0);
        println!("images: {:?}", res);
}
}
