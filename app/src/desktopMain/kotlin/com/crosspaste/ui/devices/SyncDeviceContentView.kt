package com.crosspaste.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import com.crosspaste.app.AppSize
import com.crosspaste.db.sync.SyncRuntimeInfo
import com.crosspaste.dto.sync.SyncInfo
import com.crosspaste.ui.theme.AppUISize.medium
import org.koin.compose.koinInject

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SyncDeviceContentView(
    syncInfo: SyncInfo,
    action: @Composable () -> Unit,
) {
    val appSize = koinInject<AppSize>()
    val syncRuntimeInfo = SyncRuntimeInfo.createSyncRuntimeInfo(syncInfo)

    var hover by remember { mutableStateOf(false) }
    val background =
        if (hover) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }

    DeviceBarView(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(appSize.deviceHeight)
                .background(background)
                .onPointerEvent(
                    eventType = PointerEventType.Enter,
                    onEvent = {
                        hover = true
                    },
                ).onPointerEvent(
                    eventType = PointerEventType.Exit,
                    onEvent = {
                        hover = false
                    },
                ),
        background = background,
        syncRuntimeInfo = syncRuntimeInfo,
    ) {
        Row(
            modifier =
                Modifier
                    .wrapContentSize()
                    .padding(start = medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            action()
        }
    }
}
