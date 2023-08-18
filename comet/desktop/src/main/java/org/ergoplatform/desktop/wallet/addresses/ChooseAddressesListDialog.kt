package org.ergoplatform.desktop.wallet.addresses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import org.ergoplatform.Application
import org.ergoplatform.ErgoAmount
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.uilogic.STRING_LABEL_ALL_ADDRESSES
import org.ergoplatform.uilogic.STRING_LABEL_WALLET_TOKEN_BALANCE
import org.ergoplatform.uilogic.STRING_TITLE_CHOOSE_ADDRESS
import org.ergoplatform.wallet.getBalanceForAllAddresses
import org.ergoplatform.wallet.getNumOfAddresses
import org.ergoplatform.wallet.getSortedDerivedAddressesList
import org.ergoplatform.wallet.getTokensForAllAddresses

@Composable
fun ChooseAddressesListDialog(
    wallet: Wallet,
    withAllAddresses: Boolean,
    onAddressChosen: (WalletAddress?) -> Unit,
    onDismiss: () -> Unit,
) {
    val addresses = remember(wallet) { wallet.getSortedDerivedAddressesList() }

    AppDialog(onDismiss, verticalPadding = defaultPadding * 6) {

        Box(Modifier.fillMaxWidth()) {

            val scrollState = rememberScrollState()
            Column(
                Modifier.fillMaxWidth().verticalScroll(scrollState)
            ) {

                Text(
                    remember { Application.texts.getString(STRING_TITLE_CHOOSE_ADDRESS) },
                    Modifier.padding(defaultPadding).align(Alignment.CenterHorizontally),
                    style = labelStyle(LabelStyle.BODY1)
                )

                if (withAllAddresses && addresses.size > 1) {
                    Divider(color = MosaikStyleConfig.secondaryLabelColor)

                    Column(
                        Modifier.fillMaxWidth().clickable { onAddressChosen(null) }
                            .padding(defaultPadding)
                    ) {
                        Text(
                            remember {
                                Application.texts.getString(
                                    STRING_LABEL_ALL_ADDRESSES,
                                    wallet.getNumOfAddresses()
                                )
                            },
                            Modifier.align(Alignment.CenterHorizontally),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = labelStyle(LabelStyle.BODY1BOLD),
                            color = uiErgoColor,
                        )

                        Row(
                            Modifier.padding(top = defaultPadding / 2)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = ErgoAmount(wallet.getBalanceForAllAddresses()).toComposableText(),
                                style = labelStyle(LabelStyle.BODY1BOLD)
                            )
                            val tokenNum = wallet.getTokensForAllAddresses().size
                            if (tokenNum > 0) {
                                Text(
                                    Application.texts.getString(
                                        STRING_LABEL_WALLET_TOKEN_BALANCE,
                                        tokenNum.toString()
                                    ),
                                    Modifier.padding(start = defaultPadding * 1.5f),
                                    style = labelStyle(LabelStyle.BODY1BOLD)
                                )
                            }
                        }
                    }
                }

                addresses.forEach { walletAddress ->

                    Divider(color = MosaikStyleConfig.secondaryLabelColor)

                    AddressInfoBox(
                        walletAddress,
                        wallet,
                        showFullInfo = false,
                        modifier = Modifier.fillMaxWidth().clickable {
                            onAddressChosen(walletAddress)
                        })
                }

            }
            AppScrollbar(scrollState)
        }
    }
}