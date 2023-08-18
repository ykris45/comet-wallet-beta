package org.ergoplatform.desktop.settings

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.ChildAnimator
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.Direction
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.slide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ergoplatform.Application
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.desktop.ui.canRegisterUriScheme
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.registerUriSchemes
import org.ergoplatform.uilogic.STRING_TITLE_SETTINGS
import org.ergoplatform.uilogic.settings.SettingsUiLogic

class SettingsComponent(
    private val componentContext: ComponentContext,
    navHost: NavHostComponent,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {
    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_SETTINGS)

    @OptIn(ExperimentalDecomposeApi::class)
    override fun animation(direction: Direction): ChildAnimator =
        when (direction) {
            Direction.ENTER_BACK -> slide()
            else -> fade()
        }

    private val uiLogic = SettingsUiLogic()

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        val currencyButtonTextState = remember {
            mutableStateOf(getCurrencyButtonText())
        }

        val dialogState = remember { mutableStateOf(DialogToShow.None) }

        SettingsScreen(
            currencyButtonTextState,
            onChangeCurrencyClicked = {
                WalletStateSyncManager.getInstance().fetchCurrencies()
                dialogState.value = DialogToShow.DisplayCurrencyList
            },
            onChangeConnectionSettings = {
                dialogState.value = DialogToShow.ConnectionSettings
            },
            registerUriScheme = if (canRegisterUriScheme()) ::registerUriSchemes else null,
        )

        when (dialogState.value) {
            DialogToShow.None -> {}
            DialogToShow.DisplayCurrencyList -> DisplayCurrencyListDialog(
                onDismissRequest = {
                    dialogState.value = DialogToShow.None
                },
                onCurrencyChosen = { currency ->
                    Application.prefs.prefDisplayCurrency = currency
                    WalletStateSyncManager.getInstance().invalidateCache(resetFiatValue = true)
                    currencyButtonTextState.value = getCurrencyButtonText()
                }
            )
            DialogToShow.ConnectionSettings -> ConnectionSettingsDialog(
                uiLogic,
                onStartNodeDetection = {
                    componentScope().launch(Dispatchers.IO) {
                        uiLogic.checkAvailableNodes(
                            Application.prefs
                        )
                    }
                },
                onDismissRequest = {
                    dialogState.value = DialogToShow.None
                })
        }
    }

    private fun getCurrencyButtonText() = uiLogic.getFiatCurrencyButtonText(
        Application.prefs,
        Application.texts
    )

    enum class DialogToShow {
        None,
        DisplayCurrencyList,
        ConnectionSettings,
    }
}