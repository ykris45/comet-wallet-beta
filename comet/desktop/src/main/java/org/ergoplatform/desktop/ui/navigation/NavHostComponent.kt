package org.ergoplatform.desktop.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.childAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.arkivanov.decompose.router.navigate
import com.arkivanov.decompose.router.router
import kotlinx.coroutines.flow.MutableStateFlow
import org.ergoplatform.Application
import org.ergoplatform.desktop.ergoauth.ErgoAuthComponent
import org.ergoplatform.desktop.mosaik.MosaikAppComponent
import org.ergoplatform.desktop.mosaik.MosaikOverviewComponent
import org.ergoplatform.desktop.settings.SettingsComponent
import org.ergoplatform.desktop.tokens.TokenInformationComponent
import org.ergoplatform.desktop.transactions.*
import org.ergoplatform.desktop.ui.AppBarView
import org.ergoplatform.desktop.ui.AppLockScreen
import org.ergoplatform.desktop.ui.QrScannerComponent
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.desktop.wallet.*
import org.ergoplatform.desktop.wallet.addresses.WalletAddressesComponent
import org.ergoplatform.mosaik.MosaikComposeDialog
import org.ergoplatform.mosaik.MosaikComposeDialogHandler
import org.ergoplatform.mosaik.MosaikDialog
import org.ergoplatform.mosaik.getUnreadNotificationCount
import org.ergoplatform.uilogic.STRING_TITLE_MOSAIK
import org.ergoplatform.uilogic.STRING_TITLE_SETTINGS
import org.ergoplatform.uilogic.STRING_TITLE_WALLETS
import org.ergoplatform.uilogic.STRING_ZXING_BUTTON_OK

/**
 * Navigator
 */
class NavHostComponent(
    componentContext: ComponentContext
) : Component, ComponentContext by componentContext {

    val router = router<ScreenConfig, Component>(
        initialConfiguration = ScreenConfig.WalletList,
        childFactory = ::createScreenComponent
    )

    val dialogHandler = MosaikComposeDialogHandler()

    fun showErrorDialog(message: String) {
        dialogHandler.showDialog(
            MosaikDialog(
                message,
                Application.texts.getString(STRING_ZXING_BUTTON_OK),
                null,
                null,
                null
            )
        )
    }

    fun showSnackbar(message: String) {
        snackbarText.value = message
    }

    private val snackbarText = MutableStateFlow<String?>(null)

    val lockScreen = mutableStateOf(false)

    /**
     * Factory function to create screen from given ScreenConfig
     */
    private fun createScreenComponent(
        screenConfig: ScreenConfig,
        componentContext: ComponentContext
    ): Component {

        return when (screenConfig) {

            is ScreenConfig.WalletList -> {
                WalletListComponent(
                    componentContext, this
                )
            }

            is ScreenConfig.AddWalletChooser ->
                AddWalletChooserComponent(componentContext, this)

            is ScreenConfig.AddReadOnlyWallet ->
                AddReadOnlyWalletComponent(componentContext, this)

            is ScreenConfig.CreateWallet ->
                CreateWalletComponent(componentContext, this)

            is ScreenConfig.ConfirmCreateWallet ->
                ConfirmCreateWalletComponent(componentContext, this, screenConfig.mnemonic)

            is ScreenConfig.RestoreWallet ->
                RestoreWalletComponent(componentContext, this)

            is ScreenConfig.SaveWallet ->
                SaveWalletComponent(
                    componentContext,
                    this,
                    screenConfig.mnemonic,
                    screenConfig.fromRestore
                )

            is ScreenConfig.WalletDetails ->
                WalletDetailsComponent(componentContext, this, screenConfig.walletConfig)

            is ScreenConfig.AddressTransactions ->
                AddressTransactionsComponent(
                    componentContext,
                    this,
                    screenConfig.walletConfig,
                    screenConfig.addressIdx
                )

            is ScreenConfig.WalletConfiguration ->
                WalletConfigComponent(componentContext, this, screenConfig.walletConfig)

            is ScreenConfig.WalletAddressesList ->
                WalletAddressesComponent(componentContext, this, screenConfig.walletConfig)

            is ScreenConfig.ColdSigning ->
                ColdWalletSigningComponent(
                    screenConfig.signingRequest,
                    screenConfig.walletId,
                    componentContext,
                    this
                )

            is ScreenConfig.ErgoPay ->
                ErgoPaySigningComponent(
                    screenConfig.request,
                    screenConfig.walletId,
                    screenConfig.derivationIndex,
                    screenConfig.onCompleted,
                    componentContext,
                    this
                )

            is ScreenConfig.ErgoAuth ->
                ErgoAuthComponent(
                    screenConfig.request,
                    screenConfig.walletId,
                    screenConfig.onCompleted,
                    componentContext,
                    this
                )

            is ScreenConfig.SendFunds -> SendFundsComponent(
                componentContext, this,
                screenConfig.walletConfig,
                screenConfig.derivationIndex ?: -1,
                paymentRequest = screenConfig.paymentRequest,
            )

            is ScreenConfig.ReceiveToWallet -> ReceiveToWalletComponent(
                componentContext, this, screenConfig.walletConfig, screenConfig.addressIdx
            )

            is ScreenConfig.TransactionInfo -> TransactionInfoComponent(
                screenConfig.txId, screenConfig.address, componentContext, this
            )

            is ScreenConfig.TokenInformation -> TokenInformationComponent(
                componentContext, this, screenConfig.tokenId, screenConfig.balance
            )

            is ScreenConfig.QrCodeScanner -> QrScannerComponent(
                componentContext,
                this,
                screenConfig.callback
            )

            is ScreenConfig.MosaikAppOverview -> MosaikOverviewComponent(
                componentContext, this
            )

            is ScreenConfig.MosaikApp -> MosaikAppComponent(
                screenConfig.appTitle, screenConfig.appUrl,
                componentContext, this
            )

            ScreenConfig.Settings -> SettingsComponent(
                componentContext, this
            )
        }
    }


    /**
     * Renders screen as per request
     */
    @OptIn(ExperimentalDecomposeApi::class)
    @Composable
    override fun render(scaffoldState: ScaffoldState?) {
        val navItemState = remember { mutableStateOf(NavItem.WALLETS) }

        Box {
            val drawChildren: @Composable (ScaffoldState?) -> Unit = { scaffoldState ->
                LaunchedEffect(scaffoldState) {
                    snackbarText.collect { text ->
                        text?.let {
                            scaffoldState?.snackbarHostState?.showSnackbar(text)
                            snackbarText.value = null
                        }
                    }
                }

                Children(
                    routerState = router.state,
                    animation = childAnimation { child, direction ->
                        child.instance.animation(direction)
                    }
                ) { it.instance.render(scaffoldState) }
            }

            val state = router.state.subscribeAsState()
            val navClientScreenComponent =
                state.value.activeChild.instance as? NavClientScreenComponent
            val showAppBars = navClientScreenComponent?.fullScreen != true

            if (showAppBars) {
                AppBarView(
                    navClientScreenComponent?.appBarLabel ?: "",
                    navClientScreenComponent?.actions ?: {},
                    router,
                    bottombar = { BottomBar(navItemState) }
                ) { innerPadding, scaffoldState ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        drawChildren(scaffoldState)
                    }
                }
            } else {
                drawChildren(null)
            }

            MosaikComposeDialog(dialogHandler)

            AppLockScreen(lockScreen.value)
        }
    }

    @Composable
    private fun BottomBar(navItemState: MutableState<NavItem>) {
        BottomNavigation(backgroundColor = MaterialTheme.colors.primary) {
            BottomNavigationItem(
                icon = {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = Application.texts.getString(
                            STRING_TITLE_WALLETS
                        )
                    )
                },
                selected = navItemState.value == NavItem.WALLETS,
                onClick = {
                    navItemState.value = NavItem.WALLETS
                    router.navigate { mutableListOf(ScreenConfig.WalletList) }
                },
                label = { Text(text = Application.texts.getString(STRING_TITLE_WALLETS)) },
            )
            BottomNavigationItem(
                icon = {
                    val favoriteMosaikApps =
                        Application.database.mosaikDbProvider.getAllAppFavoritesByLastVisited()
                            .collectAsState(emptyList())
                    val unread =
                        remember(favoriteMosaikApps.value) { favoriteMosaikApps.value.getUnreadNotificationCount() }

                    if (unread > 0)
                        BadgedBox(badge = {
                            Box(Modifier.padding(top = defaultPadding / 4)) {
                                Badge { Text(unread.toString()) }
                            }
                        }) {
                            Icon(
                                Icons.Default.AutoAwesomeMosaic,
                                contentDescription = Application.texts.getString(
                                    STRING_TITLE_MOSAIK
                                )
                            )
                        }
                    else
                        Icon(
                            Icons.Default.AutoAwesomeMosaic,
                            contentDescription = Application.texts.getString(STRING_TITLE_MOSAIK),
                        )
                },
                selected = navItemState.value == NavItem.MOSAIK,
                onClick = {
                    navItemState.value = NavItem.MOSAIK
                    router.navigate { mutableListOf(ScreenConfig.MosaikAppOverview) }
                },
                label = { Text(text = Application.texts.getString(STRING_TITLE_MOSAIK)) },
            )
            BottomNavigationItem(
                icon = {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = Application.texts.getString(
                            STRING_TITLE_SETTINGS
                        )
                    )
                },
                selected = navItemState.value == NavItem.SETTINGS,
                onClick = {
                    navItemState.value = NavItem.SETTINGS
                    router.navigate { listOf(ScreenConfig.Settings) }
                },
                label = { Text(text = Application.texts.getString(STRING_TITLE_SETTINGS)) },
            )
        }
    }

    private enum class NavItem {
        WALLETS, SETTINGS, MOSAIK
    }
}

