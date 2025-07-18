package com.crosspaste.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.crosspaste.app.AppSize
import com.crosspaste.config.CommonConfigManager
import com.crosspaste.dto.sync.SyncInfo
import com.crosspaste.i18n.GlobalCopywriter
import com.crosspaste.sync.NearbyDeviceManager
import com.crosspaste.ui.base.CustomSwitch
import com.crosspaste.ui.base.link
import com.crosspaste.ui.base.network
import com.crosspaste.ui.base.wifi
import com.crosspaste.ui.theme.AppUIColors
import com.crosspaste.ui.theme.AppUISize.large2X
import com.crosspaste.ui.theme.AppUISize.medium
import com.crosspaste.ui.theme.AppUISize.small2X
import com.crosspaste.ui.theme.AppUISize.xLarge
import com.crosspaste.ui.theme.AppUISize.xxxLarge
import com.crosspaste.utils.getJsonUtils
import com.crosspaste.utils.getNetUtils
import org.koin.compose.koinInject

@Composable
fun NetSettingsContentView(extContent: @Composable () -> Unit = {}) {
    val appSize = koinInject<AppSize>()
    val configManager = koinInject<CommonConfigManager>()
    val nearbyDeviceManager = koinInject<NearbyDeviceManager>()
    val copywriter = koinInject<GlobalCopywriter>()
    val netUtils = getNetUtils()
    val jsonUtils = getJsonUtils()

    var ip: String? by remember { mutableStateOf(null) }
    var port: String? by remember { mutableStateOf(null) }

    val config by configManager.config.collectAsState()

    LaunchedEffect(Unit) {
        ip = netUtils.getPreferredLocalIPAddress() ?: "N/A"
        val currentPort = config.port
        port = if (currentPort == 0) "N/A" else currentPort.toString()
    }

    Column(
        modifier =
            Modifier
                .wrapContentSize()
                .background(AppUIColors.generalBackground),
    ) {
        SettingItemsTitleView("network_info")

        SettingItemView(
            painter = network(),
            text = "ip_address",
        ) {
            ip?.let {
                SettingsText(text = it)
            } ?: run {
                CircularProgressIndicator(modifier = Modifier.size(xLarge))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(start = xxxLarge))

        SettingItemView(
            painter = link(),
            text = "port",
        ) {
            port?.let {
                SettingsText(text = it)
            } ?: run {
                CircularProgressIndicator(modifier = Modifier.size(xLarge))
            }
        }
    }

    Column(
        modifier =
            Modifier
                .wrapContentSize()
                .background(AppUIColors.generalBackground),
    ) {
        SettingItemsTitleView("service_discovery")

        SettingItemView(
            painter = wifi(),
            text = "allow_discovery_by_new_devices",
        ) {
            CustomSwitch(
                modifier =
                    Modifier
                        .width(medium * 2)
                        .height(large2X),
                checked = config.enableDiscovery,
                onCheckedChange = { newIsAllowDiscovery ->
                    configManager.updateConfig("enableDiscovery", newIsAllowDiscovery)
                },
            )
        }
    }

    Column(
        modifier =
            Modifier
                .wrapContentSize()
                .background(AppUIColors.generalBackground),
    ) {
        SettingItemsTitleView("blacklist")

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val blacklist =
                remember(config) {
                    val list: List<SyncInfo> = jsonUtils.JSON.decodeFromString(config.blacklist)
                    mutableStateListOf(*list.toTypedArray())
                }

            if (blacklist.isEmpty()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(appSize.deviceHeight)
                            .padding(start = small2X),
                    verticalArrangement = Arrangement.Center,
                ) {
                    SettingsText(text = copywriter.getText("empty"))
                }
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = appSize.deviceHeight * 3),
                    verticalArrangement = Arrangement.Top,
                ) {
                    itemsIndexed(blacklist) { index, syncInfo ->
                        val currentIndex by rememberUpdatedState(index)
                        val currentSyncInfo by rememberUpdatedState(syncInfo)

                        BlackListDeviceView(currentSyncInfo) {
                            val blackSyncInfos: List<SyncInfo> =
                                jsonUtils.JSON
                                    .decodeFromString<List<SyncInfo>>(
                                        config.blacklist,
                                    ).filter { it.appInfo.appInstanceId != currentSyncInfo.appInfo.appInstanceId }

                            val newBlackList = jsonUtils.JSON.encodeToString(blackSyncInfos)
                            configManager.updateConfig("blacklist", newBlackList)
                            blacklist.remove(currentSyncInfo)
                            nearbyDeviceManager.refreshSyncManager()
                        }

                        if (currentIndex != blacklist.size - 1) {
                            HorizontalDivider(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }

    extContent()
}
