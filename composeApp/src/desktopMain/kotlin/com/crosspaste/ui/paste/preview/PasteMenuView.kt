package com.crosspaste.ui.paste.preview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.crosspaste.app.AppWindowManager
import com.crosspaste.i18n.Copywriter
import com.crosspaste.i18n.GlobalCopywriter
import com.crosspaste.paste.PasteboardService
import com.crosspaste.realm.paste.PasteData
import com.crosspaste.realm.paste.PasteRealm
import com.crosspaste.realm.paste.PasteType
import com.crosspaste.ui.LocalPageViewContent
import com.crosspaste.ui.ScreenContext
import com.crosspaste.ui.ScreenType
import com.crosspaste.ui.base.MenuItem
import com.crosspaste.ui.base.MessageType
import com.crosspaste.ui.base.NotificationManager
import com.crosspaste.ui.base.PasteTooltipAreaView
import com.crosspaste.ui.base.PasteTypeIconView
import com.crosspaste.ui.base.TOOLTIP_TEXT_STYLE
import com.crosspaste.ui.base.UISupport
import com.crosspaste.ui.base.clipboard
import com.crosspaste.ui.base.favorite
import com.crosspaste.ui.base.getMenWidth
import com.crosspaste.ui.base.measureTextWidth
import com.crosspaste.ui.base.moreVertical
import com.crosspaste.ui.base.noFavorite
import com.crosspaste.ui.favoriteColor
import com.crosspaste.utils.getDateUtils
import com.crosspaste.utils.ioDispatcher
import com.crosspaste.utils.mainDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun PasteMenuView(
    pasteData: PasteData,
    toShow: (Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val pasteRealm = koinInject<PasteRealm>()
    val appWindowManager = koinInject<AppWindowManager>()
    val pasteboardService = koinInject<PasteboardService>()
    val copywriter = koinInject<GlobalCopywriter>()
    val notificationManager = koinInject<NotificationManager>()

    var parentBounds by remember { mutableStateOf(Rect.Zero) }
    var cursorPosition by remember { mutableStateOf(Offset.Zero) }
    var showMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var job: Job? by remember { mutableStateOf(null) }

    var showPopup by remember { mutableStateOf(false) }

    var hoverMenu by remember { mutableStateOf(false) }
    var hoverCopy by remember { mutableStateOf(false) }
    var hoverFavorite by remember { mutableStateOf(false) }
    var hoverSource by remember { mutableStateOf(false) }

    fun startShowing() {
        if (job?.isActive == true) { // Don't restart the job if it's already active
            return
        }
        job =
            scope.launch {
                showMenu = true
                toShow(true)
            }
    }

    fun hide() {
        job?.cancel()
        job = null
        showMenu = false
        toShow(false)
    }

    fun hideIfNotHovered(globalPosition: Offset) {
        if (!parentBounds.contains(globalPosition)) {
            hide()
        }
    }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .onGloballyPositioned { parentBounds = it.boundsInWindow() }
                .onPointerEvent(PointerEventType.Enter) {
                    cursorPosition = it.position
                    if (!showMenu && !it.buttons.areAnyPressed) {
                        startShowing()
                    }
                }
                .onPointerEvent(PointerEventType.Move) {
                    cursorPosition = it.position
                    if (!showMenu && !it.buttons.areAnyPressed) {
                        startShowing()
                    }
                }
                .onPointerEvent(PointerEventType.Exit) {
                    hideIfNotHovered(parentBounds.topLeft + it.position)
                }
                .clip(RoundedCornerShape(5.dp))
                .background(if (showMenu) MaterialTheme.colorScheme.surface.copy(0.72f) else Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        val menuText = copywriter.getText("menu")

        PasteTooltipAreaView(
            Modifier.fillMaxWidth().height(25.dp),
            text = menuText,
            computeTooltipPlacement = {
                val textWidth = measureTextWidth(menuText, TOOLTIP_TEXT_STYLE)
                TooltipPlacement.ComponentRect(
                    anchor = Alignment.BottomStart,
                    alignment = Alignment.BottomEnd,
                    offset = DpOffset(-textWidth - 16.dp, (-20).dp),
                )
            },
        ) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .onPointerEvent(
                            eventType = PointerEventType.Enter,
                            onEvent = {
                                hoverMenu = true
                            },
                        )
                        .onPointerEvent(
                            eventType = PointerEventType.Exit,
                            onEvent = {
                                hoverMenu = false
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (hoverMenu) {
                                    MaterialTheme.colorScheme.background
                                } else {
                                    Color.Transparent
                                },
                            ),
                ) {}
                Icon(
                    painter = moreVertical(),
                    contentDescription = "info",
                    modifier =
                        Modifier.size(18.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        showPopup = !showPopup
                                    },
                                )
                            },
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        val copyText = copywriter.getText("copy")

        if (showMenu) {
            PasteTooltipAreaView(
                Modifier.fillMaxWidth().height(25.dp),
                text = copyText,
                computeTooltipPlacement = {
                    val textWidth = measureTextWidth(copyText, TOOLTIP_TEXT_STYLE)
                    TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomStart,
                        alignment = Alignment.BottomEnd,
                        offset = DpOffset(-textWidth - 16.dp, (-20).dp),
                    )
                },
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .onPointerEvent(
                                eventType = PointerEventType.Enter,
                                onEvent = {
                                    hoverCopy = true
                                },
                            )
                            .onPointerEvent(
                                eventType = PointerEventType.Exit,
                                onEvent = {
                                    hoverCopy = false
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (hoverCopy) {
                                        MaterialTheme.colorScheme.background
                                    } else {
                                        Color.Transparent
                                    },
                                ),
                    ) {}

                    Icon(
                        modifier =
                            Modifier.size(16.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            appWindowManager.setMainCursorWait()
                                            scope.launch(ioDispatcher) {
                                                pasteboardService.tryWritePasteboard(
                                                    pasteData,
                                                    localOnly = true,
                                                    filterFile = false,
                                                )
                                                withContext(mainDispatcher) {
                                                    appWindowManager.resetMainCursor()
                                                    notificationManager.addNotification(
                                                        message = copywriter.getText("copy_successful"),
                                                        messageType = MessageType.Success,
                                                    )
                                                }
                                            }
                                        },
                                    )
                                },
                        painter = clipboard(),
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            val favoriteText = copywriter.getText(if (pasteData.favorite) "delete_favorite" else "favorite")

            PasteTooltipAreaView(
                Modifier.fillMaxWidth().height(25.dp),
                text = favoriteText,
                computeTooltipPlacement = {
                    val textWidth = measureTextWidth(favoriteText, TOOLTIP_TEXT_STYLE)
                    TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomStart,
                        alignment = Alignment.BottomEnd,
                        offset = DpOffset(-textWidth - 16.dp, (-20).dp),
                    )
                },
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .onPointerEvent(
                                eventType = PointerEventType.Enter,
                                onEvent = {
                                    hoverFavorite = true
                                },
                            )
                            .onPointerEvent(
                                eventType = PointerEventType.Exit,
                                onEvent = {
                                    hoverFavorite = false
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (hoverFavorite) {
                                        MaterialTheme.colorScheme.background
                                    } else {
                                        Color.Transparent
                                    },
                                ),
                    ) {}

                    Icon(
                        modifier =
                            Modifier.size(16.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            pasteRealm.setFavorite(pasteData.id, !pasteData.favorite)
                                        },
                                    )
                                },
                        painter = if (pasteData.favorite) favorite() else noFavorite(),
                        contentDescription = "Favorite",
                        tint = favoriteColor(),
                    )
                }
            }

            val detailInfo = getDetailInfo(copywriter, pasteData)
            PasteTooltipAreaView(
                Modifier.fillMaxWidth().height(25.dp),
                text = detailInfo,
                computeTooltipPlacement = {
                    val textWidth = measureTextWidth(detailInfo, TOOLTIP_TEXT_STYLE)
                    TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomStart,
                        alignment = Alignment.BottomEnd,
                        offset = DpOffset(-textWidth - 16.dp, (-30).dp),
                    )
                },
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .onPointerEvent(
                                eventType = PointerEventType.Enter,
                                onEvent = {
                                    hoverSource = true
                                },
                            )
                            .onPointerEvent(
                                eventType = PointerEventType.Exit,
                                onEvent = {
                                    hoverSource = false
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (hoverSource) {
                                        MaterialTheme.colorScheme.background
                                    } else {
                                        Color.Transparent
                                    },
                                ),
                    ) {}
                    PasteTypeIconView(pasteData, size = 16.dp)
                }
            }
        }
    }

    if (showPopup) {
        Popup(
            alignment = Alignment.TopEnd,
            offset =
                IntOffset(
                    with(density) { ((-40).dp).roundToPx() },
                    with(density) { (5.dp).roundToPx() },
                ),
            onDismissRequest = {
                if (showPopup) {
                    showPopup = false
                    showMenu = false
                    toShow(false)
                }
            },
            properties =
                PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                ),
        ) {
            MoreMenuItems(pasteData) {
                showPopup = false
                showMenu = false
                toShow(false)
            }
        }
    }
}

private val PointerEvent.position get() = changes.first().position

@Composable
fun MoreMenuItems(
    pasteData: PasteData,
    hideMore: () -> Unit,
) {
    val currentPage = LocalPageViewContent.current
    val copywriter = koinInject<GlobalCopywriter>()
    val pasteRealm = koinInject<PasteRealm>()
    val uiSupport = koinInject<UISupport>()
    Box(
        modifier =
            Modifier
                .wrapContentSize()
                .background(Color.Transparent)
                .shadow(15.dp),
    ) {
        val menuTexts =
            arrayOf(
                copywriter.getText("open"),
                copywriter.getText("delete"),
            )

        val maxWidth = getMenWidth(menuTexts)

        Column(
            modifier =
                Modifier
                    .width(maxWidth)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            MenuItem(copywriter.getText("open")) {
                if (pasteData.pasteType == PasteType.TEXT) {
                    hideMore()
                    currentPage.value =
                        ScreenContext(
                            ScreenType.PASTE_TEXT_EDIT,
                            currentPage.value,
                            pasteData,
                        )
                } else {
                    uiSupport.openPasteData(pasteData)
                    hideMore()
                }
            }
            MenuItem(copywriter.getText("delete")) {
                runBlocking {
                    pasteRealm.markDeletePasteData(pasteData.id)
                }
                hideMore()
            }
        }
    }
}

fun getDetailInfo(
    copywriter: Copywriter,
    pasteData: PasteData,
): String {
    val infos = mutableListOf<String>()
    pasteData.source?.let {
        infos.add(
            "${copywriter.getText("source")}: $it",
        )
    }
    val typeText =
        when (pasteData.pasteType) {
            PasteType.TEXT -> "text"
            PasteType.URL -> "link"
            PasteType.HTML -> "html"
            PasteType.IMAGE -> "image"
            PasteType.FILE -> "file"
            else -> "unknown"
        }
    infos.add(
        "${copywriter.getText("type")}: ${copywriter.getText(typeText)}",
    )
    pasteData.createTime.let {
        infos.add(
            "${copywriter.getText("create_time")}: ${copywriter.getDate(
                getDateUtils().convertRealmInstantToLocalDateTime(it),
                true,
            )}",
        )
    }
    return infos.joinToString("\n")
}