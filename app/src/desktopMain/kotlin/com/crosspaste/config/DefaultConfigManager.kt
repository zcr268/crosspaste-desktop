package com.crosspaste.config

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.crosspaste.notification.MessageType
import com.crosspaste.notification.NotificationManager
import com.crosspaste.presist.OneFilePersist
import com.crosspaste.utils.DeviceUtils
import com.crosspaste.utils.LocaleUtils

class DefaultConfigManager(
    private val configFilePersist: OneFilePersist,
    override val deviceUtils: DeviceUtils,
    private val localeUtils: LocaleUtils,
) : ConfigManager {

    override var config by mutableStateOf(
        runCatching {
            loadConfig() ?: createDefaultAppConfig()
        }.getOrElse {
            createDefaultAppConfig()
        },
    )

    override var notificationManager: NotificationManager? = null

    override fun loadConfig(): AppConfig? {
        return configFilePersist.read(AppConfig::class)
    }

    private fun createDefaultAppConfig(): AppConfig {
        return AppConfig(
            appInstanceId = deviceUtils.createAppInstanceId(),
            language = localeUtils.getLanguage(),
        )
    }

    @Synchronized
    override fun updateConfig(
        key: String,
        value: Any,
    ) {
        val oldConfig = config
        config = oldConfig.copy(key, value)
        runCatching {
            saveConfig(key, value, config)
        }.onFailure {
            notificationManager?.let { manager ->
                manager.sendNotification(
                    title = { it.getText("failed_to_save_config") },
                    messageType = MessageType.Error,
                )
            }
            config = oldConfig
        }
    }

    override fun saveConfig(
        key: String,
        value: Any,
        config: AppConfig,
    ) {
        configFilePersist.save(config)
    }
}
