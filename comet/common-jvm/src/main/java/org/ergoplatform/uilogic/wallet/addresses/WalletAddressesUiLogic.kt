package org.ergoplatform.uilogic.wallet.addresses

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.*
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.wallet.getSortedDerivedAddressesList
import org.ergoplatform.wallet.isReadOnly

abstract class WalletAddressesUiLogic {
    var wallet: Wallet? = null
        private set
    var addresses: List<WalletAddress> = emptyList()
        private set
    abstract val coroutineScope: CoroutineScope

    private var flowCollectJob: Job? = null

    fun init(database: WalletDbProvider, walletId: Int) {
        // cancel an already running job so we don't have multiple flow collections running
        flowCollectJob?.cancel()

        flowCollectJob = coroutineScope.launch {
            database.walletWithStateByIdAsFlow(walletId).collect {
                // called every time something (Room) / config (SqlDelight) changes in the DB
                wallet = it
                wallet?.let {
                    addresses = it.getSortedDerivedAddressesList()
                    LogUtils.logDebug("WalletAddresses", "New data loaded")
                    notifyNewAddresses()
                }
            }
        }
    }

    fun addNextAddresses(
        database: WalletDbProvider,
        prefs: PreferencesProvider,
        number: Int,
        signingSecrets: SigningSecrets?
    ) {
        // firing up appkit for the first time needs some time on medium end devices, so do this on
        // background thread while showing infinite progress bar
        coroutineScope.launch(Dispatchers.IO) {
            val sortedAddresses = addresses
            wallet?.let { wallet ->
                val xpubkey = wallet.walletConfig.extendedPublicKey?.let {
                    deserializeExtendedPublicKeySafe(it)
                }

                val addedAddresses = mutableListOf<String>()

                var nextIdx = 0

                // find next free slot
                val indices = sortedAddresses.map { it.derivationIndex }.toMutableList()

                notifyUiLocked(true)
                database.withTransaction {
                    for (i in 1..number) {
                        while (indices.contains(nextIdx)) {
                            nextIdx++
                        }

                        // okay, we have the next address idx - now get the address
                        // we either have the mnemonic or the xpubkey (-> canDeriveAddresses() )
                        val nextAddress = signingSecrets?.let {
                            getPublicErgoAddressFromMnemonic(
                                signingSecrets,
                                nextIdx
                            )
                        } ?: xpubkey?.let {
                            getPublicErgoAddressFromXPubKey(it, nextIdx)
                        }!!

                        // this address could be already added as a read only address - delete it
                        database.deleteWalletConfigAndStates(nextAddress)

                        database.insertWalletAddress(
                            WalletAddress(
                                0, wallet.walletConfig.firstAddress!!, nextIdx,
                                nextAddress, null
                            )
                        )
                        indices.add(nextIdx)
                        addedAddresses.add(nextAddress)
                    }
                }
                signingSecrets?.clearMemory()
                notifyUiLocked(false)
                // make NodeConnector fetch the balances of the added addresses, in case they
                // were used before
                WalletStateSyncManager.getInstance().refreshSingleAddresses(prefs, database, addedAddresses)
            }
        }
    }

    fun canDeriveAddresses(): Boolean {
        return wallet?.walletConfig?.isReadOnly() == false || wallet?.walletConfig?.extendedPublicKey != null
    }

    abstract fun notifyNewAddresses()
    abstract fun notifyUiLocked(locked: Boolean)

}