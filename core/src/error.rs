use tokio::task::JoinError;

#[derive(Debug, thiserror::Error)]
pub enum MangaError {
    #[error("internal error: {msg}")]
    InternalError { msg: String },

    #[error("network error: {msg}")]
    NetworkError { msg: String },

    #[error("database migrate error: {msg}")]
    MigrateError { msg: String },
}

impl From<uniffi::UnexpectedUniFFICallbackError> for MangaError {
    fn from(value: uniffi::UnexpectedUniFFICallbackError) -> Self {
        tracing::error!(?value, "uniffi ffi error");
        Self::InternalError {
            msg: format!("uniffi: {}", value.reason).into(),
        }
    }
}

impl From<anyhow::Error> for MangaError {
    fn from(value: anyhow::Error) -> Self {
        Self::InternalError {
            msg: value.to_string(),
        }
    }
}

impl From<reqwest::Error> for MangaError {
    fn from(value: reqwest::Error) -> Self {
        Self::NetworkError {
            msg: value.to_string(),
        }
    }
}

impl From<JoinError> for MangaError {
    fn from(value: JoinError) -> Self {
        Self::InternalError {
            msg: value.to_string(),
        }
    }
}

impl From<sqlx::Error> for MangaError {
    fn from(value: sqlx::Error) -> Self {
        Self::InternalError {
            msg: value.to_string(),
        }
    }
}

impl From<sqlx::migrate::MigrateError> for MangaError {
    fn from(value: sqlx::migrate::MigrateError) -> Self {
        Self::MigrateError {
            msg: value.to_string(),
        }
    }
}
