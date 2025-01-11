package com.crosspaste.ui.devices

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.crosspaste.sync.SyncManager
import com.crosspaste.ui.base.ExpandView
import org.koin.compose.koinInject

@Composable
fun DevicesContentView() {
    val syncManager = koinInject<SyncManager>()

    LaunchedEffect(Unit) {
        syncManager.resolveSyncs()
    }

    Box(
        modifier =
            Modifier.fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val syncRuntimeInfos by syncManager.realTimeSyncRuntimeInfos.collectAsState()

            if (syncRuntimeInfos.isNotEmpty()) {
                ExpandView(
                    title = "my_devices",
                    horizontalPadding = 0.dp,
                    defaultExpand = true,
                ) {
                    Spacer(modifier = Modifier.height(3.dp))
                    MyDevicesView(syncRuntimeInfos)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            ExpandView(
                title = "add_device_manually",
                horizontalPadding = 0.dp,
                defaultExpand = false,
            ) {
                Spacer(modifier = Modifier.height(3.dp))
                AddDeviceManuallyView()
            }
            Spacer(modifier = Modifier.height(10.dp))
            ExpandView(
                title = "nearby_devices",
                horizontalPadding = 0.dp,
                defaultExpand = true,
            ) {
                Spacer(modifier = Modifier.height(3.dp))
                NearbyDevicesView()
            }
        }
    }
}
