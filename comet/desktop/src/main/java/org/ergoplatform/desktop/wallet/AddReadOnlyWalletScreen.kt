package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.appTextFieldColors
import org.ergoplatform.compose.settings.primaryButtonColors
import org.ergoplatform.compose.settings.secondaryButtonColors
import org.ergoplatform.desktop.ui.AppScrollingLayout
import org.ergoplatform.desktop.ui.defaultMaxWidth
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.desktop.ui.uiErgoColor
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.*

@Composable
fun AddReadOnlyWalletScreen(
    walletAddress: MutableState<TextFieldValue>,
    walletName: MutableState<TextFieldValue>,
    errorMsg: MutableState<String?>,
    onScanAddress: () -> Unit,
    onAddClicked: () -> Unit,
    onBack: () -> Unit,
) {

    AppScrollingLayout {
        Card(
            Modifier.padding(defaultPadding).align(Alignment.Center)
                .defaultMinSize(400.dp, 200.dp)
                .widthIn(max = defaultMaxWidth)
        ) {

            Column(Modifier.padding(defaultPadding)) {
                Text(
                    Application.texts.getString(STRING_LABEL_READONLY_WALLET),
                    style = labelStyle(LabelStyle.HEADLINE2),
                    color = uiErgoColor
                )

                Text(
                    Application.texts.getString(STRING_INTRO_ADD_READONLY),
                    Modifier.padding(top = defaultPadding),
                    style = labelStyle(LabelStyle.BODY1)
                )

                OutlinedTextField(
                    walletAddress.value,
                    onValueChange = {
                        walletAddress.value = it
                        errorMsg.value = null
                    },
                    Modifier.fillMaxWidth().padding(top = defaultPadding),
                    maxLines = 1,
                    singleLine = true,
                    isError = errorMsg.value != null,
                    label = { Text(Application.texts.getString(STRING_LABEL_WALLET_ADDRESS)) },
                    trailingIcon = {
                        IconButton(onClick = onScanAddress) {
                            Icon(Icons.Default.QrCodeScanner, null)
                        }
                    },
                    colors = appTextFieldColors(),
                )

                errorMsg.value?.let {
                    Text(it, style = labelStyle(LabelStyle.BODY2), color = uiErgoColor)
                }

                OutlinedTextField(
                    walletName.value,
                    onValueChange = {
                        walletName.value = it
                    },
                    Modifier.fillMaxWidth().padding(top = defaultPadding),
                    maxLines = 1,
                    singleLine = true,
                    label = { Text(Application.texts.getString(STRING_LABEL_WALLET_NAME)) },
                    colors = appTextFieldColors(),
                )

                Row(Modifier.align(Alignment.End).padding(top = defaultPadding)) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.padding(end = defaultPadding),
                        colors = secondaryButtonColors()
                    ) {
                        Text(Application.texts.getString(STRING_BUTTON_BACK))
                    }

                    Button(
                        onClick = { onAddClicked() },
                        colors = primaryButtonColors()
                    ) {
                        Text(Application.texts.getString(STRING_MENU_ADD_WALLET))
                    }
                }
            }
        }
    }

}