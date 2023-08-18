package org.ergoplatform.mosaik

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.addressbook.getAddressLabelFromDatabase
import org.ergoplatform.api.OkHttpSingleton
import org.ergoplatform.isValidErgoAddress
import org.ergoplatform.mosaik.model.MosaikContext
import org.ergoplatform.mosaik.model.MosaikManifest
import org.ergoplatform.persistance.CacheFileManager
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.uilogic.*
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.formatFiatToString
import org.ergoplatform.utils.getHostname
import org.ergoplatform.utils.normalizeUrl
import org.ergoplatform.wallet.getDerivedAddress
import org.ergoplatform.wallet.getSortedDerivedAddressesList
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.math.max

abstract class AppMosaikRuntime(
    val appName: String,
    val appVersionName: String,
    val platformType: () -> MosaikContext.Platform,
    val getLocale: () -> Locale?,
    val guidManager: MosaikGuidManager,
) : MosaikRuntime(
    OkHttpBackendConnector(
        OkHttpSingleton.getInstance().newBuilder(),
        getContextFor = { url ->
            MosaikContext(
                MosaikContext.LIBRARY_MOSAIK_VERSION,
                guidManager.getGuidForHost(getHostname(url)),
                (getLocale() ?: Locale.getDefault()).language,
                appName,
                appVersionName,
                platformType(),
                (TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000),
            )
        }
    )
) {

    lateinit var appDatabase: IAppDatabase
    var cacheFileManager: CacheFileManager? = null
    var preferencesProvider: PreferencesProvider? = null

    init {
        MosaikLogger.logger = { severity, msg, throwable ->
            LogUtils.logDebug("Mosaik", "$severity: $msg", throwable)
        }

        appLoaded = { manifest ->
            coroutineScope.launch {
                saveVisitToDb(manifest)
                appNotLoadedUrl = null
                onAppNavigated(manifest)
                onRefreshFavorite()
            }
        }
    }

    private suspend fun saveVisitToDb(manifest: MosaikManifest) {
        val normalizedUrl = normalizedAppUrl!!

        val formerAppEntry = appDatabase.mosaikDbProvider.loadAppEntry(normalizedUrl)

        val lastVisit = formerAppEntry?.lastVisited ?: 0
        val timeStampNow = System.currentTimeMillis()
        val fileName = formerAppEntry?.iconFile ?: UUID.randomUUID().toString()

        if (manifest.iconUrl != null &&
            (timeStampNow - lastVisit > 1000L * 60 * 30
                    || cacheFileManager?.fileExists(fileName) == false
                    || manifest.appName != formerAppEntry?.name)
        ) {
            cacheFileManager?.let { cacheFileManager ->
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val image =
                            backendConnector.fetchImage(manifest.iconUrl!!, appUrl!!, appUrl)
                        if (image.size <= 250000)
                            cacheFileManager.saveFileContent(fileName, image)
                    } catch (t: Throwable) {
                        // Image could not be loaded
                        LogUtils.logDebug("AppMosaikRuntime", "Error saving appicon", t)
                    }
                }
            }
        } else if (manifest.iconUrl == null) {
            // delete icon file
            cacheFileManager?.deleteFile(fileName)
        }

        val newAppEntry = MosaikAppEntry(
            normalizedUrl,
            manifest.appName,
            manifest.appDescription,
            iconFile = fileName,
            timeStampNow,
            favorite = formerAppEntry?.favorite ?: false,
            manifest.notificationCheckUrl,
            formerAppEntry?.lastNotificationMessage,
            lastNotificationMs = formerAppEntry?.lastNotificationMs ?: 0,
            nextNotificationCheck = max(
                formerAppEntry?.nextNotificationCheck ?: 0,
                System.currentTimeMillis() + MosaikNotificationSyncManager.MIN_CHECK_INTERVAL_MINUTES * 1000L * 60
            ),
            notificationUnread = false,
        )

        isFavoriteApp = newAppEntry.favorite

        appDatabase.mosaikDbProvider.insertOrUpdateAppEntry(newAppEntry)
        val hostName = getHostname(appUrl!!)
        appDatabase.mosaikDbProvider.insertOrUpdateAppHost(
            MosaikAppHost(
                hostName,
                guidManager.getGuidForHost(hostName)
            )
        )
    }

    var isFavoriteApp = false

    abstract fun onAppNavigated(manifest: MosaikManifest)

    abstract fun onRefreshFavorite()

    abstract fun appNotLoaded(cause: Throwable)

    fun loadUrlEnteredByUser(appUrl: String) {
        val loadUrl =
            if (!appUrl.contains(' ') && !appUrl.contains("://"))
                "https://$appUrl"
            else
                appUrl

        loadMosaikApp(loadUrl)
    }

    fun canSwitchFavorite(): Boolean =
        normalizedAppUrl != null || appNotLoadedUrl != null && isFavoriteApp

    fun switchFavorite() {
        val appUrlToUse = appNotLoadedUrl?.let { normalizeUrl(it) } ?: normalizedAppUrl

        appUrlToUse?.let { appUrl ->
            coroutineScope.launch {
                appDatabase.mosaikDbProvider.loadAppEntry(appUrl)?.let { appEntry ->
                    val newEntry = appEntry.copy(favorite = !appEntry.favorite)
                    appDatabase.mosaikDbProvider.insertOrUpdateAppEntry(newEntry)
                    isFavoriteApp = newEntry.favorite
                    onRefreshFavorite()
                }

            }
        }
    }

    override fun showError(error: Throwable) {
        showDialog(
            MosaikDialog(
                getUserErrorMessage(error),
                stringProvider.getString(STRING_ZXING_BUTTON_OK)
            )
        )
    }

    private var appNotLoadedUrl: String? = null

    override fun appNotLoadedError(appUrl: String, error: Throwable) {
        runBlocking {
            val appDbEntry = appDatabase.mosaikDbProvider.loadAppEntry(normalizeUrl(appUrl))
            isFavoriteApp = appDbEntry?.favorite ?: false
            appNotLoadedUrl = appUrl

            appNotLoaded(error)
            onRefreshFavorite()
        }
    }

    fun retryLoadingLastAppNotLoaded() {
        appNotLoadedUrl?.let { loadUrlEnteredByUser(it) }
    }

    private val normalizedAppUrl get() = appUrl?.let { normalizeUrl(it) }

    lateinit var stringProvider: StringProvider

    override fun formatString(string: StringConstant, values: String?): String {
        val appConstant = when (string) {
            StringConstant.ChooseAddress -> STRING_BUTTON_CHOOSE_ADDRESS
            StringConstant.PleaseChoose -> STRING_DROPDOWN_CHOOSE_OPTION
            StringConstant.ChooseWallet -> STRING_BUTTON_CHOOSE_WALLET
        }
        return values?.let {
            stringProvider.getString(appConstant, values)
        } ?: stringProvider.getString(appConstant)

    }

    override val fiatRate: Double?
        get() {
            val walletStateManager = WalletStateSyncManager.getInstance()
            return if (walletStateManager.hasFiatValue)
                walletStateManager.fiatValue.value.toDouble()
            else null
        }

    override fun convertErgToFiat(nanoErg: Long, formatted: Boolean): String? {
        val walletStateManager = WalletStateSyncManager.getInstance()
        return if (walletStateManager.hasFiatValue) {
            val fiatAmount = ErgoAmount(nanoErg).toDouble() * walletStateManager.fiatValue.value
            if (formatted) {
                formatFiatToString(
                    fiatAmount,
                    walletStateManager.fiatCurrency,
                    stringProvider,
                )
            } else BigDecimal(fiatAmount).setScale(2, RoundingMode.HALF_UP).toString()
        } else null
    }

    override fun getErgoWalletLabel(firstAddress: String): String? =
        runBlocking {
            val walletConfig = guidManager.appDatabase.walletDbProvider.loadWalletByFirstAddress(
                firstAddress
            )
            walletConfig?.displayName
        }

    override suspend fun getErgoAddressLabel(ergoAddress: String): String? =
        getAddressLabelFromDatabase(guidManager.appDatabase, ergoAddress, stringProvider)

    override fun isErgoAddressValid(ergoAddress: String): Boolean =
        isValidErgoAddress(ergoAddress)

    override var preferFiatInput: Boolean
        get() = preferencesProvider?.isSendInputFiatAmount ?: false
        set(value) {
            preferencesProvider?.isSendInputFiatAmount = value
        }

    fun getUserErrorMessage(errorCause: Throwable): String {
        return when (errorCause) {
            is ConnectionException ->
                stringProvider.getString(STRING_ERROR_MOSAIK_CONNECTION)
            is NoMosaikAppException ->
                stringProvider.getString(STRING_ERROR_NO_MOSAIK_APP, errorCause.url)
            is InvalidValuesException -> errorCause.message!!
            else ->
                stringProvider.getString(
                    STRING_ERROR_LOADING_MOSAIK_APP,
                    errorCause.javaClass.simpleName + " " + errorCause.message
                )
        }
    }

    private var valueIdWalletChosen: String? = null
    private var valueIdAddressChosen: String? = null

    override fun showErgoAddressChooser(valueId: String) {
        valueIdAddressChosen = valueId
        valueIdWalletChosen = null
        startWalletChooser()
    }

    override fun showErgoWalletChooser(valueId: String) {
        valueIdWalletChosen = valueId
        valueIdAddressChosen = null
        startWalletChooser()
    }

    abstract fun startWalletChooser()

    var walletForAddressChooser: Wallet? = null
        private set

    abstract fun startAddressIdxChooser()

    fun onWalletChosen(wallet: Wallet) {
        valueIdWalletChosen?.let { valueId ->
            setValue(
                valueId,
                wallet.getSortedDerivedAddressesList().map { it.publicAddress })
            valueIdWalletChosen = null
        }

        valueIdAddressChosen?.let {
            val addresses = wallet.getSortedDerivedAddressesList()
            walletForAddressChooser = wallet
            if (addresses.size == 1)
                onAddressChosen(addresses.first().derivationIndex)
            else {
                startAddressIdxChooser()
            }
        }
    }

    fun onAddressChosen(addressIdx: Int) {
        setValue(valueIdAddressChosen!!, walletForAddressChooser!!.getDerivedAddress(addressIdx))
        valueIdAddressChosen = null
    }

    fun resetAppData() {
        appUrl?.let {
            coroutineScope.launch {
                guidManager.resetAppData(getHostname(it))
                loadMosaikApp(it)
            }
        }
    }
}

class MosaikGuidManager {
    lateinit var appDatabase: IAppDatabase

    private val guidMap = HashMap<String, String>()

    /**
     * returns guid for a given hostname from cache map, database or newly calculated
     */
    internal fun getGuidForHost(hostname: String): String {
        return if (guidMap.containsKey(hostname)) {
            guidMap[hostname]!!
        } else {
            val hostInfo = runBlocking {
                appDatabase.mosaikDbProvider.getMosaikHostInfo(hostname)
            }

            return if (hostInfo != null) {
                hostInfo.guid
            } else {
                val guid = createNewGuid(hostname)
                guid
            }
        }
    }

    private fun createNewGuid(hostname: String): String {
        val guid = UUID.randomUUID().toString()
        guidMap[hostname] = guid
        return guid
    }

    suspend fun resetAppData(hostname: String) {
        appDatabase.mosaikDbProvider.getMosaikHostInfo(hostname)?.let {
            val newGuid = createNewGuid(hostname)
            appDatabase.mosaikDbProvider.insertOrUpdateAppHost(it.copy(guid = newGuid))
        }
    }
}