package org.unknownplace.manga.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.unknownplace.manga.ui.theme.MangaTheme

@Composable
fun ChapterListItem(
    id: Long,
    title: String,
    modifier: Modifier = Modifier,
    isRead: Boolean = false,
    onClick: (id: Long) -> Unit
) {
    Card(
        modifier = modifier,
        onClick = { onClick(id) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth()
        ) {
            if (isRead) {
                Box(modifier = Modifier.size(16.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                    )
                }
            }
            Text(
                text = title,
                color = if (!isRead) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = .5f)
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ChapterListItemPreview() {
    MangaTheme {
        Surface {
            ChapterListItem(id = 123, title = "Chapter", onClick = {})
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ChapterListItemIsReadPreview() {
    MangaTheme {
        Surface {
            ChapterListItem(id = 123, title = "Chapter", isRead = true, onClick = {})
        }
    }
}

