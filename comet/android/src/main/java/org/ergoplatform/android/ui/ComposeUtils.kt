package org.ergoplatform.android.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ergoplatform.android.R
import org.ergoplatform.compose.ComposePlatformUtils
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle

@Composable
fun AppComposeTheme(content: @Composable () -> Unit) {
    prepareMosaikConfig()
    initComposePlatformUtils()

    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = colorResource(id = R.color.primary),
            secondary = colorResource(id = R.color.primary),
            surface = colorResource(id = R.color.cardview_background),
            onSurface = colorResource(id = R.color.text_color),
            error = colorResource(id = R.color.primary),
            isLight = LocalContext.current.resources.getBoolean(R.bool.isLight)
        ),
        typography = MaterialTheme.typography.copy(
            body1 = labelStyle(LabelStyle.BODY1),
            button = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 18.sp.times(0.0892857143)
            ),
        ),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colorResource(id = R.color.text_color),
            LocalContentAlpha provides 1f,
            content = content
        )
    }
}

@Composable
private fun prepareMosaikConfig() {
    MosaikStyleConfig.apply {
        primaryLabelColor = colorResource(id = R.color.primary)
        secondaryLabelColor = colorResource(id = R.color.darkgrey)
        defaultLabelColor = colorResource(id = R.color.text_color)
        primaryButtonTextColor = colorResource(id = R.color.textcolor)
        secondaryButtonTextColor = colorResource(id = R.color.textcolor)
        secondaryButtonColor = colorResource(id = R.color.secondary)
        textButtonTextColor = colorResource(id = R.color.primary)
        textButtonColorDisabled = secondaryLabelColor
        textFieldBackgroundColor = colorResource(id = R.color.cardview_background)
        cardShapeRadius = 10.dp
        buttonShapeRadius = 16.dp
        buttonPadding = PaddingValues(defaultPadding, 10.dp)
    }
}

private fun initComposePlatformUtils() {
    ComposePlatformUtils.getDrawablePainter = {
        painterResource(
            when (it) {
                ComposePlatformUtils.Drawable.Octagon -> R.drawable.ic_octagon_48
                ComposePlatformUtils.Drawable.NftImage -> R.drawable.ic_photo_camera_24
                ComposePlatformUtils.Drawable.NftAudio -> R.drawable.ic_music_note_24
                ComposePlatformUtils.Drawable.NftVideo -> R.drawable.ic_videocam_24
            }
        )
    }
}