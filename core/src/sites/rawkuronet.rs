use std::collections::HashMap;

use anyhow::bail;
use async_trait::async_trait;
use lazy_static::lazy_static;
use regex::Regex;
use scraper::{ElementRef, Html, Selector};

use crate::rt;

use super::{Link, MangaSite};

#[derive(Debug, Clone)]
pub struct RawkuroNet {
    client: reqwest::Client,
}

impl Default for RawkuroNet {
    fn default() -> Self {
        let client = reqwest::ClientBuilder::new()
            .user_agent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36")
            .build().expect("failed to initialize http client for jmanga");
        Self { client }
    }
}

#[async_trait]
impl MangaSite for RawkuroNet {
    fn name(&self) -> String {
        "rawkuro.net".to_string()
    }

    async fn search(&self, text: String) -> anyhow::Result<Vec<Link>> {
        let copy = self.clone();
        rt().spawn(async move {
            let mut query = HashMap::new();
            query.insert("keyword", text);

            let res = copy.client.get("https://rawkuro.net/search")
                .query(&query)
                .send().await?;
            let res = res.text().await?;

            let doc = Html::parse_document(&res);
            let sel = Selector::parse("#main a").unwrap();
            let img_sel = Selector::parse("img").unwrap();

            let results: Vec<Link> = doc.select(&sel)
                .filter_map(|a| {
                    let title = a.attr("title");
                    let link = a.attr("href");
                    let img = a.select(&img_sel).next().and_then(|i| i.attr("data-src"));

                    if let (Some(title), Some(link)) = (title, link) {
                        if copy.can_handle_chapters(link) {
                            Some(Link {
                                text: title.to_string(),
                                url: link.to_string(),
                                image: img.and_then(|i| Some(format!("https://rawkuro.net{}", i))),
                            })
                        } else {
                            None
                        }
                    } else {
                        None
                    }
                }).collect();
            Ok(results)
        }).await?
    }

    fn can_handle_chapters(&self, url: &str) -> bool {
        lazy_static! {
            static ref RE: Regex = Regex::new(r"^https://rawkuro.net/manga/[^/]+$").unwrap();
        }
        RE.is_match(url)
    }

    async fn chapters(&self, url: &str) -> anyhow::Result<Vec<Link>> {
        let res = self.client.get(url).send().await?;
        let res = res.text().await?;

        let doc = Html::parse_document(&res);
        let sel = Selector::parse("#myUL li a").unwrap();

        let results: Vec<Link> = doc.select(&sel)
            .filter_map(|a| {
                let title = a.text().next();
                let link = a.attr("href");
                if let (Some(title), Some(link)) = (title, link) {
                    Some(Link {
                        text: title.trim().to_string(),
                        url: link.to_string(),
                        image: None,
                    })
                } else {
                    None
                }
            }).collect();
        Ok(results)
    }

    fn can_handle_images(&self, url: &str) -> bool {
        lazy_static! {
            static ref RE: Regex = Regex::new(r"^https://rawkuro.net/manga/[^/]+/[^/]+$").unwrap();
        }
        RE.is_match(url)
    }

    async fn images(&self, url: &str) -> anyhow::Result<Vec<String>> {
        lazy_static! {
            static ref RE: Regex = Regex::new(r"const\s+CHAPTER_ID\s+=\s+(\d+);").unwrap();
        }

        let res = self.client.get(url).send().await?;
        let res = res.text().await?;

        let captures = match RE.captures(&res) {
            Some(c) => c,
            None => bail!("no chapter id found"),
        };
        let id = match captures.get(1).and_then(|m| Some(m.as_str())) {
            Some(s) => s,
            None => bail!("no chapter id found"),
        };

        let res = self.client.get(format!("https://rawkuro.net/ajax/image/list/chap/{}", id))
            .send().await?;
        let res = res.text().await?;
        let res: serde_json::Value = serde_json::from_str(&res)?;

        let res = match res.get("html").and_then(|v| v.as_str()) {
            Some(v) => v,
            None => bail!("invalid response: {}", res),
        };

        let doc = Html::parse_fragment(res);
        let sel = Selector::parse(".separator").unwrap();
        let a_sel = Selector::parse(".readImg").unwrap();

        struct WithIndex<'a> {
            element: ElementRef<'a>,
            index: u64,
        }

        let mut links: Vec<WithIndex> = doc.select(&sel)
            .filter_map(|e| {
                if let Some(index) = e.attr("data-index").and_then(|v| Some(v.parse::<u64>().ok()?)) {
                    Some(WithIndex {
                        element: e,
                        index,
                    })
                } else {
                    None
                }
            }).collect();
        links.sort_by(|a, b| a.index.cmp(&b.index));

        let links: Vec<String> = links.iter().flat_map(|w| {
            w.element.select(&a_sel)
                .filter_map(|a| {
                    a.attr("href").and_then(|t| Some(t.to_string()))
                }).collect::<Vec<_>>()
        }).collect();

        Ok(links)
    }
}

#[cfg(test)]
mod tests {
    use std::sync::Arc;

    use crate::sites::MangaSite;

    use super::RawkuroNet;

    #[tokio::test]
    async fn test_rawkuronet_search() {
        let s: Arc<dyn MangaSite> = Arc::new(RawkuroNet::default());

        let res = s.search("異世界".into()).await;
        assert!(res.is_ok());
        let res = res.unwrap();
        assert!(res.len() > 0);
        println!("search results: {}", res.len());
        println!("results[0]: {:?}", res[0]);
    }

    #[tokio::test]
    async fn test_rawkuronet_chapters() {
        let s: Arc<dyn MangaSite> = Arc::new(RawkuroNet::default());

        let res = s.search("異世界".into()).await;
        assert!(res.is_ok());
        let res = res.unwrap();
        assert!(res.len() > 0);
        println!("search results: {}", res.len());
        println!("results[0]: {:?}", res[0]);

        let res = s.chapters(&res[0].url).await;
        assert!(res.is_ok());
        let res = res.unwrap();
        assert!(res.len() > 0);
        println!("chapters: {}", res.len());
        println!("chapters[0]: {:?}", res[0]);
    }

    #[tokio::test]
    async fn test_rawkuronet_images() {
        let s: Arc<dyn MangaSite> = Arc::new(RawkuroNet::default());

        let res = s.search("異世界".into()).await;
        assert!(res.is_ok());
        let res = res.unwrap();
        assert!(res.len() > 0);
        println!("search results: {}", res.len());
        println!("results[0]: {:?}", res[0]);

        let res = s.chapters(&res[0].url).await;
        assert!(res.is_ok());
        let res = res.unwrap();
        assert!(res.len() > 0);
        println!("chapters: {}", res.len());
        println!("chapters[0]: {:?}", res[0]);

        let res = s.images(&res[0].url).await;
        assert!(res.is_ok());
        let res = res.unwrap();
        assert!(res.len() > 0);
        println!("images: {:?}", res);
    }
}
