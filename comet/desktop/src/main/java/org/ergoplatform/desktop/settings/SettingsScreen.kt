package org.ergoplatform.desktop.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.AppButton
import org.ergoplatform.compose.settings.AppCard
import org.ergoplatform.compose.settings.secondaryButtonColors
import org.ergoplatform.desktop.appVersionString
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.*

@Composable
fun SettingsScreen(
    currencyButtonTextState: MutableState<String>,
    onChangeCurrencyClicked: () -> Unit,
    onChangeConnectionSettings: () -> Unit,
    registerUriScheme: (() -> Unit)?,
) {
    Column {
        Column(Modifier.fillMaxWidth()) {
            Image(
                ergoLogo(),
                null,
                colorFilter = ColorFilter.tint(MosaikStyleConfig.secondaryButtonTextColor),
                modifier = Modifier.height(90.dp).align(Alignment.CenterHorizontally)
                    .padding(defaultPadding)
            )
            Text(
                Application.texts.getString(STRING_APP_NAME),
                Modifier.align(Alignment.CenterHorizontally),
                style = labelStyle(LabelStyle.HEADLINE1)
            )
            Text(
                appVersionString,
                Modifier.align(Alignment.CenterHorizontally),
                style = labelStyle(LabelStyle.BODY2)
            )
            Text(
                Application.texts.getString(
                    STRING_DESC_ABOUT, Application.texts.getString(
                        STRING_ABOUT_YEAR
                    )
                ),
                Modifier.padding(defaultPadding / 2).align(Alignment.CenterHorizontally),
                style = labelStyle(LabelStyle.BODY1)
            )
            LinkifyText(
                Application.texts.getString(STRING_DESC_ABOUT_MOREINFO),
                Modifier.padding(
                    bottom = defaultPadding,
                    start = defaultPadding / 2,
                    end = defaultPadding / 2
                ).align(Alignment.CenterHorizontally),
                style = labelStyle(LabelStyle.BODY1),
                isHtml = true
            )
        }

        Box {
            val scrollState = rememberScrollState()

            Column(
                Modifier.fillMaxSize().verticalScroll(scrollState).padding(defaultPadding)
            ) {

                // Fiat currency
                AppCard(
                    Modifier.widthIn(max = defaultMaxWidth).align(Alignment.CenterHorizontally)
                ) {
                    Column(Modifier.padding(defaultPadding)) {
                        LinkifyText(
                            Application.texts.getString(STRING_DESC_COINGECKO),
                            Modifier.padding(defaultPadding / 2)
                                .align(Alignment.CenterHorizontally),
                            style = labelStyle(LabelStyle.BODY1),
                            isHtml = true
                        )

                        Button(
                            onClick = { onChangeCurrencyClicked() },
                            colors = secondaryButtonColors(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(currencyButtonTextState.value)
                        }
                    }

                }

                // NFT settings
                AppCard(
                    Modifier.widthIn(max = defaultMaxWidth).align(Alignment.CenterHorizontally)
                        .padding(top = defaultPadding)
                ) {
                    Column(Modifier.padding(defaultPadding)) {
                        Text(
                            remember { Application.texts.getString(STRING_DESC_TOKEN_SETTINGS) },
                            Modifier.align(Alignment.CenterHorizontally)
                                .padding(bottom = defaultPadding),
                            style = labelStyle(LabelStyle.BODY1BOLD),
                        )

                        LinkifyText(
                            Application.texts.getString(STRING_DESC_TOKEN_VERIFICATION),
                            Modifier.padding(defaultPadding / 2)
                                .align(Alignment.CenterHorizontally),
                            style = labelStyle(LabelStyle.BODY1),
                            textAlignment = TextAlign.Center,
                            isHtml = true
                        )

                        Divider(
                            Modifier.padding(vertical = defaultPadding),
                            color = MosaikStyleConfig.secondaryLabelColor
                        )

                        Text(
                            remember { Application.texts.getString(STRING_DESC_DOWNLOAD_CONTENT) },
                            Modifier.fillMaxWidth().padding(defaultPadding / 2),
                            textAlign = TextAlign.Center
                        )

                        val isDownloadContentState =
                            remember { mutableStateOf(Application.prefs.downloadNftContent) }
                        AppButton(
                            {
                                isDownloadContentState.value = !isDownloadContentState.value
                                Application.prefs.downloadNftContent = isDownloadContentState.value
                            },
                            Modifier.align(Alignment.CenterHorizontally),
                            colors = secondaryButtonColors(),
                        ) {
                            Text(
                                Application.texts.getString(
                                    if (isDownloadContentState.value) STRING_BUTTON_DOWNLOAD_CONTENT_OFF
                                    else STRING_BUTTON_DOWNLOAD_CONTENT_ON
                                )
                            )
                        }
                    }

                }

                // Register URI schemes
                registerUriScheme?.let {
                    AppCard(
                        Modifier.widthIn(max = defaultMaxWidth).align(Alignment.CenterHorizontally)
                            .padding(top = defaultPadding)
                    ) {
                        Column(Modifier.padding(defaultPadding)) {
                            Text(
                                remember {
                                    Application.texts.getString(STRING_DESC_REGISTER_HANDLER)
                                },
                                Modifier.padding(defaultPadding / 2)
                                    .align(Alignment.CenterHorizontally),
                                style = labelStyle(LabelStyle.BODY1),
                                textAlign = TextAlign.Center,
                            )

                            Button(
                                onClick = registerUriScheme,
                                colors = secondaryButtonColors(),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(remember {
                                    Application.texts.getString(STRING_BUTTON_REGISTER_HANDLER)
                                })
                            }
                        }

                    }
                }

                // Expert settings
                AppCard(
                    Modifier.widthIn(max = defaultMaxWidth).align(Alignment.CenterHorizontally)
                        .padding(top = defaultPadding)
                ) {
                    Column(Modifier.padding(defaultPadding)) {
                        Text(
                            Application.texts.getString(STRING_DESC_EXPERT_SETTINGS),
                            Modifier.padding(defaultPadding / 2)
                                .align(Alignment.CenterHorizontally),
                            style = labelStyle(LabelStyle.BODY1),
                        )

                        Button(
                            onClick = { onChangeConnectionSettings() },
                            colors = secondaryButtonColors(),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = defaultPadding),
                        ) {
                            Text(Application.texts.getString(STRING_BUTTON_CONNECTION_SETTINGS))
                        }
                    }

                }

            }
            AppScrollbar(scrollState)
        }
    }
}