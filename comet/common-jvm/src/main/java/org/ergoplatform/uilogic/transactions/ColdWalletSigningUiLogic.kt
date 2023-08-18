package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.ErgoFacade
import org.ergoplatform.SigningSecrets
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.transactions.*
import org.ergoplatform.uilogic.STRING_ERROR_COLD_QR_CODE_DOES_NOT_FIT
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.getMessageOrName
import org.ergoplatform.wallet.getSortedDerivedAddressesList

abstract class ColdWalletSigningUiLogic {
    abstract val coroutineScope: CoroutineScope

    var state = State.SCANNING
        private set
    val qrPagesCollector = QrCodePagesCollector(::getColdSigningRequestChunk)
    var signedQrCode: String? = null
        private set
    private var signingRequest: PromptSigningResult? = null

    var wallet: Wallet? = null
        private set

    var transactionInfo: TransactionInfo? = null
        private set
    var lastErrorMessage: String? = null
        private set

    /**
     * Adds QR code chunk when applicable
     *
     * @return TransactionInfo when it could be built, null otherwise. In case no info is built there
     *         might be warnings or error messages available in lastErrorMessage.
     */
    fun addQrCodeChunk(qrCodeChunk: String, texts: StringProvider): TransactionInfo? {

        // are we are already done? => don't add
        if (transactionInfo != null) {
            return transactionInfo
        }

        val added = qrPagesCollector.addPage(qrCodeChunk)
        lastErrorMessage =
            if (added) null else texts.getString(STRING_ERROR_COLD_QR_CODE_DOES_NOT_FIT)

        transactionInfo = buildRequestWhenApplicable()
        transactionInfo?.let { state = State.WAITING_TO_CONFIRM }

        return transactionInfo
    }

    private fun buildRequestWhenApplicable(): TransactionInfo? {
        if (qrPagesCollector.hasAllPages()) {
            try {
                val sr = coldSigningRequestFromQrChunks(qrPagesCollector.getAllPages())
                signingRequest = sr
                return sr.buildTransactionInfo()
            } catch (t: Throwable) {
                LogUtils.logDebug("ColdWalletSigning", "Error thrown on signing", t)
                val message = t.getMessageOrName()
                lastErrorMessage = "Error: $message"
            }
        }

        return null
    }

    fun setWalletId(walletId: Int, db: WalletDbProvider) {
        coroutineScope.launch {
            wallet = db.loadWalletWithStateById(walletId)
        }
    }

    fun signTxWithMnemonicAsync(signingSecrets: SigningSecrets, texts: StringProvider) {
        signingRequest?.let { signingRequest ->
            val derivedAddresses =
                wallet!!.getSortedDerivedAddressesList().map { it.derivationIndex }

            coroutineScope.launch {
                val ergoTxResult: SigningResult
                withContext(Dispatchers.IO) {
                    ergoTxResult = ErgoFacade.signSerializedErgoTx(
                        signingRequest.serializedTx!!, signingSecrets,
                        derivedAddresses, texts
                    )
                    signedQrCode = buildColdSigningResponse(ergoTxResult)
                    signingSecrets.clearMemory()
                }
                notifyUiLocked(false)

                if (ergoTxResult.success && signedQrCode != null) {
                    state = State.PRESENT_RESULT
                }
                notifySigningResult(ergoTxResult)
            }

            notifyUiLocked(true)
        }
    }

    abstract fun notifyUiLocked(locked: Boolean)
    abstract fun notifySigningResult(ergoTxResult: SigningResult)

    enum class State { SCANNING, WAITING_TO_CONFIRM, PRESENT_RESULT }
}