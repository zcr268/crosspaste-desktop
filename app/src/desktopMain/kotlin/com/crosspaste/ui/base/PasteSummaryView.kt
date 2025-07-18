package com.crosspaste.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.crosspaste.app.DesktopAppSize
import com.crosspaste.db.paste.PasteData
import com.crosspaste.i18n.GlobalCopywriter
import com.crosspaste.ui.theme.AppUISize.small3X
import com.crosspaste.ui.theme.AppUISize.tiny3X
import com.crosspaste.ui.theme.AppUISize.tinyRoundedCornerShape
import org.koin.compose.koinInject

@Composable
fun PasteSummaryView(
    pasteData: PasteData,
    selected: Boolean,
    onPress: () -> Unit,
    onDoubleTap: () -> Unit,
) {
    val appSize = koinInject<DesktopAppSize>()
    val copywriter = koinInject<GlobalCopywriter>()
    val loading = copywriter.getText("loading")
    val unknown = copywriter.getText("unknown")

    var summary by remember { mutableStateOf(loading) }

    LaunchedEffect(pasteData.pasteState) {
        summary = pasteData.getSummary(loading, unknown)
    }

    val background =
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(appSize.centerSearchPasteSummaryHeight),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = small3X)
                    .clip(tinyRoundedCornerShape)
                    .background(background)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                onPress()
                            },
                            onDoubleTap = {
                                onDoubleTap()
                            },
                        )
                    },
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = tiny3X),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PasteTypeIconView(
                    pasteData = pasteData,
                    tint = MaterialTheme.colorScheme.contentColorFor(background),
                    background = background,
                )

                Text(
                    modifier = Modifier.padding(start = small3X),
                    text = summary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.contentColorFor(background),
                    textAlign = TextAlign.Start,
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Light,
                            lineHeight = TextUnit.Unspecified,
                        ),
                )
            }
        }
    }
}
