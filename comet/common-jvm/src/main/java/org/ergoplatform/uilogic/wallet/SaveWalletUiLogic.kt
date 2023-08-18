package org.ergoplatform.uilogic.wallet

import kotlinx.coroutines.*
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.SigningSecrets
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.getPublicErgoAddressFromMnemonic
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.uilogic.STRING_LABEL_WALLET_DEFAULT
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.utils.LogUtils

class SaveWalletUiLogic(val mnemonic: SecretString, private val fromRestore: Boolean) {

    // Constructor

    private var useDeprecatedDerivation: Boolean = false

    var publicAddress: String = getPublicErgoAddressFromMnemonic(signingSecrets)
        private set

    val hasAlternativeAddress = fromRestore &&
            (!publicAddress.equals(
                getPublicErgoAddressFromMnemonic(
                    SigningSecrets(mnemonic, true)
                )
            ))

    private val derivedAddressesFound = emptyList<String>().toMutableList()
    private var derivedAddressesSearchJob: Job? = null

    // methods

    val signingSecrets get() = SigningSecrets(mnemonic, useDeprecatedDerivation)

    fun switchAddress() {
        if (hasAlternativeAddress) {
            useDeprecatedDerivation = !useDeprecatedDerivation
            derivedAddressesSearchJob?.cancel()
            derivedAddressesFound.clear()
            derivedAddressesSearchJob = null
            publicAddress = getPublicErgoAddressFromMnemonic(signingSecrets)
        }
    }

    suspend fun startDerivedAddressesSearch(
        ergoApiService: ApiServiceManager,
        walletDbProvider: WalletDbProvider,
        callback: (Int) -> Unit
    ) {
        // derived addresses search only done
        // - when restoring an existing wallet
        // - and it is not already in db
        if (!fromRestore || getExistingWallet(walletDbProvider) != null) {
            return
        }

        derivedAddressesSearchJob?.cancel()
        coroutineScope {
            derivedAddressesSearchJob = launch(Dispatchers.IO) {
                var derivedAddressIdx = 1
                var histFound = true
                try {
                    while (histFound) {
                        val derivedAddress =
                            getPublicErgoAddressFromMnemonic(signingSecrets, derivedAddressIdx)

                        val nodeHistFound = if (ergoApiService.preferNodeAsExplorer) try {
                            ergoApiService.getNodeConfirmedTransactionsForAddress(
                                derivedAddress, 1, 0
                            ).execute().body()!!.items.isNotEmpty() && isActive
                        } catch (t: Throwable) {
                            false
                        }
                        else false

                        histFound = nodeHistFound ||
                                ergoApiService.getConfirmedTransactionsForAddress(
                                    derivedAddress,
                                    1,
                                    0
                                )
                                    .execute().body()!!.items.isNotEmpty() && isActive

                        if (histFound) {
                            derivedAddressesFound.add(derivedAddress)
                            callback(derivedAddressIdx)
                            derivedAddressIdx++
                        }
                    }
                } catch (t: Throwable) {
                    LogUtils.logDebug(
                        this.javaClass.simpleName,
                        "Error searching derived addresses: ${t.message}", t
                    )
                }
            }
        }

    }

    suspend fun getSuggestedDisplayName(
        walletDbProvider: WalletDbProvider,
        strings: StringProvider
    ) = getExistingWallet(walletDbProvider)?.displayName
        ?: strings.getString(STRING_LABEL_WALLET_DEFAULT)

    private suspend fun getExistingWallet(walletDbProvider: WalletDbProvider) =
        walletDbProvider.loadWalletByFirstAddress(publicAddress)

    /**
     * show display name input text field only when there are already wallets set up
     */
    fun showSuggestedDisplayName(walletDbProvider: WalletDbProvider) =
        walletDbProvider.getAllWalletConfigsSynchronous().isNotEmpty()

    /**
     * Saves the wallet data to DB
     */
    suspend fun suspendSaveToDb(
        walletDbProvider: WalletDbProvider,
        displayName: String,
        encType: Int,
        secretStorage: ByteArray?
    ) {
        val publicAddress = this.publicAddress

        // check if the wallet already exists
        val existingWallet = getExistingWallet(walletDbProvider)

        if (existingWallet != null) {
            // update encType and secret storage, removes existing xpubkey
            val walletConfig = WalletConfig(
                existingWallet.id,
                displayName,
                existingWallet.firstAddress,
                encType,
                secretStorage,
                extendedPublicKey = null
            )
            walletDbProvider.updateWalletConfig(walletConfig)
        } else {
            val walletConfig =
                WalletConfig(
                    0,
                    displayName,
                    publicAddress,
                    encType,
                    secretStorage,
                    extendedPublicKey = null
                )
            walletDbProvider.insertWalletConfig(walletConfig)

            // add derived addresses, if we've found some
            derivedAddressesFound.forEachIndexed { idx, nextAddress ->
                // this address could be already added as a read only address - delete it
                walletDbProvider.deleteWalletConfigAndStates(nextAddress)
                walletDbProvider.insertWalletAddress(
                    WalletAddress(
                        0, publicAddress, idx + 1,
                        nextAddress, null
                    )
                )
            }

            WalletStateSyncManager.getInstance().invalidateCache()
        }
    }

    fun isPasswordWeak(password: SecretString?): Boolean {
        return password == null || password.data.size < 8
    }

    fun eraseSecrets() {
        // it is enough to erase mnemonic: all SigningSecrets contain them
        mnemonic.erase()
    }
}