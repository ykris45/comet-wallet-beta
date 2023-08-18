package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.compose.settings.AppCard
import org.ergoplatform.compose.settings.mediumIconSize
import org.ergoplatform.compose.settings.smallIconSize
import org.ergoplatform.compose.tokens.TokenEntryViewData
import org.ergoplatform.compose.tokens.TokenLabel
import org.ergoplatform.desktop.tokens.TokenEntryView
import org.ergoplatform.desktop.tokens.TokenFilterMenu
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.desktop.wallet.addresses.ChooseAddressButton
import org.ergoplatform.mosaik.MiddleEllipsisText
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.tokens.getTokenErgoValueSum
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.transactions.AddressTransactionWithTokens
import org.ergoplatform.uilogic.transactions.getTransactionStateString
import org.ergoplatform.uilogic.wallet.WalletDetailsUiLogic
import org.ergoplatform.utils.formatFiatToString
import org.ergoplatform.utils.formatTokenPriceToString
import org.ergoplatform.utils.millisecondsToLocalTime

@Composable
fun WalletDetailsScreen(
    uiLogic: WalletDetailsUiLogic,
    informationVersion: Int,
    downloadingTransactions: Boolean,
    onScanClicked: () -> Unit,
    onChooseAddressClicked: () -> Unit,
    onReceiveClicked: () -> Unit,
    onSendClicked: () -> Unit,
    onAddressesClicked: () -> Unit,
    onTransactionClicked: (String, String) -> Unit,
    onViewTransactionsClicked: () -> Unit,
    onTokenClicked: (String, Long?) -> Unit,
) {
    AppScrollingLayout {
        val firstColumnContent = @Composable {
            SelectAddressLayout(
                uiLogic,
                onChooseAddressClicked,
                onScanClicked,
                onReceiveClicked,
                onSendClicked,
                onAddressesClicked
            )

            ErgoBalanceLayout(uiLogic)

            if (uiLogic.hasTokens) {
                WalletTokensLayout(uiLogic, onTokenClicked)
            }
        }
        val secondColumnContent = @Composable {
            TransactionsLayout(
                informationVersion,
                downloadingTransactions,
                uiLogic,
                onViewTransactionsClicked,
                onTransactionClicked,
                onTokenClicked = { onTokenClicked(it, null) }
            )
        }

        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            if (maxWidth < 900.dp) {
                Column(Modifier.widthIn(min = 400.dp, max = defaultMaxWidth)) {
                    firstColumnContent()
                    secondColumnContent()
                }
            } else {
                Row(Modifier.widthIn(max = defaultMaxWidth * 2)) {
                    Column(Modifier.align(Alignment.Top).weight(1f)) {
                        firstColumnContent()
                    }
                    Column(Modifier.align(Alignment.Top).weight(1f)) {
                        secondColumnContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectAddressLayout(
    uiLogic: WalletDetailsUiLogic,
    onChooseAddressClicked: () -> Unit,
    onScanClicked: () -> Unit,
    onReceiveClicked: () -> Unit,
    onSendClicked: () -> Unit,
    onAddressesClicked: () -> Unit
) {
    AppCard(Modifier.padding(defaultPadding)) {
        Column(Modifier.padding(defaultPadding)) {

            Row {
                Icon(
                    Icons.Default.AltRoute,
                    null,
                    Modifier.requiredSize(mediumIconSize).align(Alignment.CenterVertically)
                )

                Column(Modifier.weight(1f).padding(start = defaultPadding)) {

                    Text(remember { Application.texts.getString(STRING_TITLE_WALLET_ADDRESS) })

                    ChooseAddressButton(
                        uiLogic.walletAddress,
                        uiLogic.wallet,
                        onClick = onChooseAddressClicked
                    )

                }

                IconButton(onScanClicked, Modifier.align(Alignment.Top)) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        null,
                        Modifier.requiredSize(smallIconSize)
                    )
                }
            }

            Row(Modifier.padding(top = defaultPadding / 2)) {
                Box(Modifier.requiredWidth(mediumIconSize))

                IconButton(onReceiveClicked, Modifier.weight(1f)) {
                    Icon(Icons.Default.CallReceived, null)
                }

                IconButton(onSendClicked, Modifier.weight(1f)) {
                    Icon(Icons.Default.CallMade, null)
                }

                IconButton(onAddressesClicked, Modifier.weight(1f)) {
                    Icon(Icons.Default.FormatListNumbered, null)
                }

            }
        }
    }
}

@Composable
private fun ErgoBalanceLayout(uiLogic: WalletDetailsUiLogic) {
    AppCard(Modifier.padding(defaultPadding)) {
        Column(Modifier.padding(defaultPadding)) {
            Row {
                Box(Modifier.requiredWidth(mediumIconSize))

                Text(
                    remember { Application.texts.getString(STRING_TITLE_WALLET_BALANCE) },
                    Modifier.weight(1f).padding(start = defaultPadding),
                    style = labelStyle(LabelStyle.BODY1BOLD),
                    color = uiErgoColor,
                )
            }

            val balanceErgoAmount = uiLogic.getErgoBalance()

            Row {
                Icon(
                    painterResource("ic_erglogo_filled.xml"), null,
                    Modifier.requiredSize(mediumIconSize)
                )

                val unconfirmed = uiLogic.getUnconfirmedErgoBalance().nanoErgs != 0L

                Text(
                    Application.texts.getString(
                        STRING_LABEL_ERG_AMOUNT, balanceErgoAmount.toString()
                    ) + (if (unconfirmed) "*" else ""),
                    Modifier.padding(start = defaultPadding).weight(1f),
                    style = labelStyle(LabelStyle.HEADLINE1)
                )

            }

            val walletSyncManager = WalletStateSyncManager.getInstance()
            if (walletSyncManager.hasFiatValue)
                Row {
                    Box(Modifier.requiredWidth(mediumIconSize))

                    Text(
                        formatFiatToString(
                            balanceErgoAmount.toDouble() * walletSyncManager.fiatValue.value,
                            walletSyncManager.fiatCurrency, Application.texts
                        ),
                        Modifier.padding(start = defaultPadding).weight(1f),
                        color = MosaikStyleConfig.secondaryLabelColor,
                        style = labelStyle(LabelStyle.BODY1)
                    )


                }

        }
    }
}

@Composable
private fun WalletTokensLayout(
    uiLogic: WalletDetailsUiLogic,
    onTokenClicked: (String, Long?) -> Unit,
) {
    AppCard(Modifier.padding(defaultPadding)) {
        Column(Modifier.padding(defaultPadding)) {

            // HEADER
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource("ic_tokenlogo.xml"), null,
                    Modifier.requiredSize(mediumIconSize)
                )

                Text(
                    uiLogic.tokensList.size.toString(),
                    Modifier.padding(start = defaultPadding),
                    style = labelStyle(LabelStyle.HEADLINE2),
                )

                Text(
                    Application.texts.getString(STRING_LABEL_TOKENS),
                    Modifier.padding(start = defaultPadding / 2).weight(1f),
                    style = labelStyle(LabelStyle.BODY1BOLD),
                    color = uiErgoColor
                )

                val walletSyncManager = WalletStateSyncManager.getInstance()
                val tokenValueToShow = getTokenErgoValueSum(
                    uiLogic.tokensList,
                    walletSyncManager
                )
                if (!tokenValueToShow.isZero()) {
                    Text(
                        text = formatTokenPriceToString(
                            tokenValueToShow,
                            walletSyncManager,
                            Application.texts
                        ),
                        style = labelStyle(LabelStyle.BODY1),
                        color = MosaikStyleConfig.secondaryLabelColor,
                        modifier = Modifier.align(Alignment.CenterVertically)
                            .padding(horizontal = defaultPadding / 2),
                    )
                }

                TokenFilterMenu(uiLogic)
            }

            // TOKENS LIST

            uiLogic.tokensList.forEach { walletToken ->

                key(walletToken) {
                    val data = TokenEntryViewData(walletToken, true, Application.texts)
                    data.bind(uiLogic.tokenInformation[walletToken.tokenId])

                    Column(Modifier.fillMaxWidth().padding(top = defaultPadding).clickable {
                        onTokenClicked(walletToken.tokenId!!, walletToken.amount)
                    }) {

                        TokenLabel(
                            data,
                            Modifier.align(Alignment.CenterHorizontally),
                            labelStyle = LabelStyle.BODY1BOLD,
                        )

                        data.displayedId?.let {
                            MiddleEllipsisText(
                                it,
                                Modifier.align(Alignment.CenterHorizontally),
                                color = MosaikStyleConfig.secondaryLabelColor
                            )
                        }

                        data.price?.let {
                            Text(
                                it, Modifier.align(Alignment.CenterHorizontally),
                                color = MosaikStyleConfig.secondaryLabelColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionsLayout(
    informationVersion: Int,
    downloadingTransactions: Boolean,
    uiLogic: WalletDetailsUiLogic,
    onViewTransactionsClicked: () -> Unit,
    onTransactionClicked: (String, String) -> Unit,
    onTokenClicked: (String) -> Unit,
) {
    AppCard(Modifier.padding(defaultPadding)) {
        Column {

            // HEADER
            Row(Modifier.padding(defaultPadding)) {
                Icon(
                    Icons.Default.CompareArrows, null,
                    Modifier.requiredSize(mediumIconSize)
                )

                Text(
                    remember { Application.texts.getString(STRING_TITLE_TRANSACTIONS) },
                    Modifier.padding(start = defaultPadding)
                        .align(Alignment.CenterVertically).weight(1f),
                    style = labelStyle(LabelStyle.BODY1BOLD),
                    color = uiErgoColor,
                )
            }

            val transactionList =
                remember { mutableStateOf(emptyList<AddressTransactionWithTokens>()) }
            LaunchedEffect(informationVersion, downloadingTransactions) {
                transactionList.value = uiLogic.loadTransactionsToShow(
                    Application.database.transactionDbProvider
                )
            }

            if (transactionList.value.isEmpty()) {
                Divider()

                Text(
                    remember { Application.texts.getString(STRING_TRANSACTIONS_NONE_YET) },
                    Modifier.padding(defaultPadding).fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            transactionList.value.forEach { transaction ->
                Divider()

                key(transaction) {
                    AddressTransactionInfo(
                        transaction,
                        onTransactionClicked = {
                            onTransactionClicked(
                                transaction.addressTransaction.txId,
                                transaction.addressTransaction.address
                            )
                        },
                        onTokenClicked
                    )
                }
            }

            if (transactionList.value.size == uiLogic.maxTransactionsToShow) {
                Divider()

                Text(
                    remember { Application.texts.getString(STRING_TRANSACTIONS_VIEW_MORE) },
                    Modifier.clickable { onViewTransactionsClicked() }.padding(defaultPadding)
                        .fillMaxWidth(),
                    style = labelStyle(LabelStyle.BODY1BOLD),
                    textAlign = TextAlign.Center,
                    color = uiErgoColor,
                )

            }

        }
    }
}

@Composable
fun AddressTransactionInfo(
    transaction: AddressTransactionWithTokens,
    onTransactionClicked: () -> Unit,
    onTokenClicked: (String) -> Unit,
) {

    Column(Modifier.clickable { onTransactionClicked() }.padding(defaultPadding)) {

        Row {
            val txHeader = transaction.addressTransaction
            Text(
                txHeader.getTransactionStateString(Application.texts),
                style = labelStyle(LabelStyle.BODY1BOLD),
                color = uiErgoColor
            )

            if (txHeader.timestamp > 0) {
                Text(
                    millisecondsToLocalTime(txHeader.timestamp),
                    Modifier.padding(start = defaultPadding).weight(1f),
                    textAlign = TextAlign.End,
                )
            }
        }

        Row(Modifier.padding(top = defaultPadding / 2)) {
            val txHeader = transaction.addressTransaction
            Text(
                txHeader.ergAmount.toComposableText(trimTrailingZeros = true),
                Modifier.align(Alignment.Top).padding(start = defaultPadding),
                style = labelStyle(LabelStyle.BODY1BOLD),
            )

            txHeader.message?.let {
                Text(
                    it,
                    Modifier.padding(start = defaultPadding).weight(1f).align(Alignment.Top),
                )
            }
        }

        transaction.tokens.forEach { token ->
            TokenEntryView(
                token.tokenAmount.toStringUsFormatted(),
                token.name,
                Modifier.padding(horizontal = defaultPadding)
                    .clickable { onTokenClicked(token.tokenId) }
            )

        }

    }
}

