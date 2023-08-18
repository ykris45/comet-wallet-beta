package org.ergoplatform.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.arkivanov.decompose.router.Router
import com.arkivanov.decompose.router.pop
import org.ergoplatform.compose.settings.AppCard
import org.ergoplatform.compose.settings.AppProgressIndicator
import org.ergoplatform.compose.settings.defaultMaxWidth
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.desktop.ui.navigation.Component
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.isErgoMainNet
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import java.awt.event.KeyEvent

val uiErgoColor get() = if (isErgoMainNet) Color(0xffff3b30) else Color(0xff4284FF)
val secondaryColor = Color(24, 25, 29)
val defaultPadding = 16.dp
val defaultMaxWidth = 600.dp
val bigIconSize = 58.dp

private val DarkColors = darkColors(
    primary = uiErgoColor,
    secondary = uiErgoColor,
    surface = secondaryColor,
    onPrimary = Color.White,
    onSecondary = Color.White
)

@Composable
fun DesktopTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = DarkColors,
        typography = Typography(
            defaultFontFamily = FontFamily(Font("google_sans_regular.ttf"))
        ).copy(
            body1 = labelStyle(LabelStyle.BODY1),
            button = TextStyle(fontSize = 18.sp),
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
    }
}

@Composable
private fun WalletAppBar(
    title: String,
    router: Router<ScreenConfig, Component>,
    actions: @Composable RowScope.() -> Unit
) {
    CompositionLocalProvider(LocalElevationOverlay provides null) {
        TopAppBar(
            backgroundColor = MaterialTheme.colors.primary,
            title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            actions = actions,
            navigationIcon = if (router.state.value.backStack.isEmpty()) null else ({
                AppBackButton(onClick = {
                    if ((router.state.value.activeChild.instance as? NavClientScreenComponent)?.onNavigateBack() != true)
                        router.pop()
                })
            })
        )
    }
}

@Composable
fun AppBarView(
    title: String,
    actions: @Composable RowScope.() -> Unit,
    router: Router<ScreenConfig, Component>,
    bottombar: @Composable () -> Unit,
    content: @Composable (PaddingValues, ScaffoldState) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        topBar = { WalletAppBar(title, router, actions = actions) },
        scaffoldState = scaffoldState,
        content = { paddingValues -> content(paddingValues, scaffoldState) },
        bottomBar = bottombar
    )
}

@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    maxWidth: Dp = defaultMaxWidth,
    verticalPadding: Dp = defaultPadding,
    content: @Composable () -> Unit
) {
    Popup(
        onDismissRequest = onDismissRequest,
        popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = IntOffset.Zero
        },
        focusable = true,
        onKeyEvent = {
            if (it.awtEventOrNull?.keyCode == KeyEvent.VK_ESCAPE) {
                onDismissRequest()
                true
            } else {
                false
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onDismissRequest) {
                    detectTapGestures(onPress = { onDismissRequest() })
                }.background(MaterialTheme.colors.background.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            AppCard(
                modifier = Modifier.padding(horizontal = defaultPadding, vertical = verticalPadding)
                    .widthIn(min = 400.dp, max = maxWidth)
                    .heightIn(min = 200.dp)
                    .noRippleClickable {
                        // no action, prevents dismissal
                    }

            ) {
                content()
            }
        }
    }
}

@Composable
fun AppBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = null
        )
    }
}

@Composable
fun AppScrollingLayout(
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable BoxScope.() -> Unit
) {
    Box {
        Box(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            content()
        }

        AppScrollbar(scrollState)
    }
}

@Composable
fun BoxScope.AppScrollbar(scrollState: ScrollState) {
    VerticalScrollbar(
        modifier = Modifier.align(Alignment.CenterEnd)
            .fillMaxHeight(),
        adapter = rememberScrollbarAdapter(scrollState)
    )
}

@Composable
fun AppLockScreen(locked: Boolean) {
    if (locked)
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
                .noRippleClickable {
                    // needed to grab user interaction
                }) {
            AppProgressIndicator()
        }
}
