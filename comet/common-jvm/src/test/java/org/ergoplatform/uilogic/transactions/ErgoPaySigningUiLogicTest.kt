package org.ergoplatform.uilogic.transactions

import junit.framework.TestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.TestPreferencesProvider
import org.ergoplatform.TestStringProvider
import org.ergoplatform.explorer.client.model.OutputInfo
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.transactions.STATIC_ERGO_PAY_URI
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.STRING_LABEL_ERROR_OCCURED
import org.ergoplatform.uilogic.STRING_LABEL_MESSAGE_FROM_DAPP
import org.ergoplatform.uilogic.TestUiWallet
import org.ergoplatform.utils.LogUtils
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.EmptyCoroutineContext


class ErgoPaySigningUiLogicTest : TestCase() {

    override fun setUp() {
        LogUtils.logDebug = true
    }

    fun testStatic() {
        runBlocking {
            // test everything is fine
            buildUiLogic(STATIC_ERGO_PAY_URI).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)

                waitWhileFetching(this)

                assertEquals(ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION, state)
                assertNotNull(epsr)
                assertNotNull(epsr?.reducedTx)
                assertNull(epsr?.message)
                assertEquals(MessageSeverity.NONE, epsr?.messageSeverity)
                assertTrue(epsr!!.addressesToUse.isEmpty())
                assertNull(epsr?.replyToUrl)
            }

            // test invalid static reduced tx given
            buildUiLogic("ergopay:thisisinvalid").apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNull(epsr)
                assertNull(transactionInfo)
                assertEquals(MessageSeverity.ERROR, getDoneSeverity())
                assertEquals(STRING_LABEL_ERROR_OCCURED, getDoneMessage(TestStringProvider()))
            }

            // test with invalid boxes
            buildUiLogic(STATIC_ERGO_PAY_URI, apiFailure = true).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNotNull(epsr)
                assertNotNull(epsr?.reducedTx)
                assertNull(transactionInfo)
                assertEquals(MessageSeverity.ERROR, getDoneSeverity())
                assertEquals(STRING_LABEL_ERROR_OCCURED, getDoneMessage(TestStringProvider()))
            }
        }
    }

    private fun prepareServer(): MockWebServer {
        val server = MockWebServer()
        val RESPONSE_SUCCESS =
            "{\"message\":\"Here is your 1 ERG round trip.\",\"messageSeverity\":\"INFORMATION\",\"address\":\"${TestUiWallet.firstAddress}\",\"reducedTx\":\"vAIBsY8KufROXCUQyk05e7UCm4hdh-afd4BXlhyy-i7xo8YAAAACUUCDoXD8c0BxwHdI_0RJQGBmVDF71RaGUSDtcClSqxuW1lsZOJnBVNdbw6VbLTwjEfCfArnW7S2skTyxDDgwgAOAlOvcAwAIzQKya10Oh0WJx8V1Smpq9AgIH___4xEYT0SiW4DH8rxitYSUCQAAwIQ9EAUEAAQADjYQAgSQAQjNAnm-Zn753LusVaBilc6HCwcCm_zbLc4o2VnygVsW-BeY6gLRkqOajMenAXMAcwEQAQIEAtGWgwMBk6OMx7KlcwAAAZPCsqVzAQB0cwJzA4MBCM3urJOxpXMEhJQJAADZptHA6wkACM0CsmtdDodFicfFdUpqavQICB___-MRGE9EoluAx_K8YrWElAkCAPgKAeC00S8AzQKya10Oh0WJx8V1Smpq9AgIH___4xEYT0SiW4DH8rxitZ1PrGY=\"}"
        server.enqueue(MockResponse().setBody(RESPONSE_SUCCESS))
        server.enqueue(MockResponse().setBody("hello, world!"))
        server.enqueue(MockResponse().setBody("{\"wrongmessage\":\"Out of order. Please try again later.\",\"messageSeverity\":\"WARNING\"}"))
        server.enqueue(MockResponse().setBody("{\"message\":\"Here is your 1 ERG round trip.\",\"messageSeverity\":\"INFORMATION\",\"address\":\"someAddressNotHere\",\"reducedTx\":\"vAIBsY8KufROXCUQyk05e7UCm4hdh-afd4BXlhyy-i7xo8YAAAACUUCDoXD8c0BxwHdI_0RJQGBmVDF71RaGUSDtcClSqxuW1lsZOJnBVNdbw6VbLTwjEfCfArnW7S2skTyxDDgwgAOAlOvcAwAIzQKya10Oh0WJx8V1Smpq9AgIH___4xEYT0SiW4DH8rxitYSUCQAAwIQ9EAUEAAQADjYQAgSQAQjNAnm-Zn753LusVaBilc6HCwcCm_zbLc4o2VnygVsW-BeY6gLRkqOajMenAXMAcwEQAQIEAtGWgwMBk6OMx7KlcwAAAZPCsqVzAQB0cwJzA4MBCM3urJOxpXMEhJQJAADZptHA6wkACM0CsmtdDodFicfFdUpqavQICB___-MRGE9EoluAx_K8YrWElAkCAPgKAeC00S8AzQKya10Oh0WJx8V1Smpq9AgIH___4xEYT0SiW4DH8rxitZ1PrGY=\"}"))
        server.enqueue(MockResponse().setBody("{\"message\":\"Out of order. Please try again later.\",\"messageSeverity\":\"WARNING\"}"))
        server.enqueue(MockResponse().setBody(RESPONSE_SUCCESS))
        server.enqueue(MockResponse().setBody("")) // <- address request supported
        server.enqueue(MockResponse().setBody(RESPONSE_SUCCESS))
        return server
    }

    fun testDynamic() {
        val server = prepareServer()

        runBlocking {
            server.start()
            val url = "ergopay://" + server.url("/").toString().substringAfter("http://")

            // actual thing going through
            buildUiLogic(url).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION, state)
                assertNotNull(epsr)
                assertNotNull(epsr?.reducedTx)
                assertNotNull(epsr?.message)
                assertEquals(MessageSeverity.INFORMATION, epsr?.messageSeverity)
                assertEquals(listOf(TestUiWallet.firstAddress), epsr?.addressesToUse)
                assertNull(epsr?.replyToUrl)

            }

            // hello, world will fail
            buildUiLogic(url).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNull(epsr)
                assertNull(transactionInfo)
                assertEquals(MessageSeverity.ERROR, getDoneSeverity())
                assertEquals(STRING_LABEL_ERROR_OCCURED, getDoneMessage(TestStringProvider()))

            }

            // response without message and without reducedTx => fail
            buildUiLogic(url).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNull(epsr)
                assertNull(transactionInfo)
                assertEquals(MessageSeverity.ERROR, getDoneSeverity())
                assertEquals(STRING_LABEL_ERROR_OCCURED, getDoneMessage(TestStringProvider()))

            }

            // wrong address - fail
            buildUiLogic(url).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNotNull(epsr)
                assertNotNull(transactionInfo)
                assertEquals(MessageSeverity.ERROR, getDoneSeverity())
                assertEquals(STRING_LABEL_ERROR_OCCURED, getDoneMessage(TestStringProvider()))
            }

            // no reduced tx, but information message
            buildUiLogic(url).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNotNull(epsr)
                assertNull(transactionInfo)
                assertEquals(MessageSeverity.WARNING, getDoneSeverity())
                assertEquals(STRING_LABEL_MESSAGE_FROM_DAPP, getDoneMessage(TestStringProvider()))
            }

            // request with address - goes through because only one address on wallet
            buildUiLogic("$url?address=#P2PK_ADDRESS#").apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION, state)
                assertNotNull(epsr)
                assertNotNull(epsr?.reducedTx)
                assertNotNull(epsr?.message)
            }

            // request with address, but two derived addresses configured: ask user
            buildUiLogic("$url?address=#P2PK_ADDRESS#", twoAddresses = true).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.WAIT_FOR_ADDRESS, state)
                assertNull(epsr)
                assertNull(transactionInfo)
                derivedAddressIdx = 0
                derivedAddressIdChanged(TestPreferencesProvider(), TestStringProvider(), db)
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION, state)
                assertNotNull(epsr)
                assertNotNull(epsr?.reducedTx)
                assertNotNull(epsr?.message)
            }

            server.shutdown()
        }
    }

    fun testDynamicWithoutWallet() {
        val server = prepareServer()

        runBlocking {
            server.start()
            val url = "ergopay://" + server.url("/").toString().substringAfter("http://")

            // actual thing going through by selecting the right wallet
            buildUiLogic(url, initWithWallet = false).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION, state)
                assertNotNull(epsr)
                assertNotNull(epsr?.reducedTx)
                assertNotNull(epsr?.message)
                assertEquals(MessageSeverity.INFORMATION, epsr?.messageSeverity)
                assertEquals(listOf(TestUiWallet.firstAddress), epsr?.addressesToUse)
                assertNull(epsr?.replyToUrl)

            }

            // hello, world will fail
            buildUiLogic(url, initWithWallet = false).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNull(epsr)
                assertNull(transactionInfo)
                assertEquals(MessageSeverity.ERROR, getDoneSeverity())
                assertEquals(STRING_LABEL_ERROR_OCCURED, getDoneMessage(TestStringProvider()))

            }

            // response without message and without reducedTx => fail
            buildUiLogic(url, initWithWallet = false).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNull(epsr)
                assertNull(transactionInfo)
                assertEquals(MessageSeverity.ERROR, getDoneSeverity())
                assertEquals(STRING_LABEL_ERROR_OCCURED, getDoneMessage(TestStringProvider()))

            }

            // wrong address - fail
            buildUiLogic(url, initWithWallet = false).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNotNull(epsr)
                assertNotNull(transactionInfo)
                assertEquals(MessageSeverity.ERROR, getDoneSeverity())
                assertEquals(STRING_LABEL_ERROR_OCCURED, getDoneMessage(TestStringProvider()))
            }

            // no reduced tx, but information message
            buildUiLogic(url, initWithWallet = false).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNotNull(epsr)
                assertNull(transactionInfo)
                assertEquals(MessageSeverity.WARNING, getDoneSeverity())
                assertEquals(STRING_LABEL_MESSAGE_FROM_DAPP, getDoneMessage(TestStringProvider()))
            }

            // the next two should not go into WAIT_FOR_WALLET in the real app. they do here because
            // getAllConfigsSync is not implemented in the mock

            // request with address - goes through because only one address on wallet
            buildUiLogic("$url?address=#P2PK_ADDRESS#", initWithWallet = false).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.WAIT_FOR_WALLET, state)
                assertNull(epsr)
                setWalletId(
                    TestUiWallet.singleAddressWallet.walletConfig.id,
                    TestPreferencesProvider(),
                    TestStringProvider(),
                    db
                )
                delay(100) // give some time to make the "db" access
                assertEquals(ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION, state)
                assertNotNull(epsr)
                assertNotNull(epsr?.reducedTx)
                assertNotNull(epsr?.message)
            }

            // request with address, but two derived addresses configured: ask user
            buildUiLogic(
                "$url?address=#P2PK_ADDRESS#",
                twoAddresses = true,
                initWithWallet = false
            ).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.WAIT_FOR_WALLET, state)
                assertNull(epsr)
                assertNull(transactionInfo)
                setWalletId(
                    TestUiWallet.singleAddressWallet.walletConfig.id,
                    TestPreferencesProvider(),
                    TestStringProvider(),
                    db
                )
                delay(100) // give some time to make the "db" access
                assertEquals(ErgoPaySigningUiLogic.State.WAIT_FOR_ADDRESS, state)
                derivedAddressIdx = 0
                derivedAddressIdChanged(TestPreferencesProvider(), TestStringProvider(), db)
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION, state)
                assertNotNull(epsr)
                assertNotNull(epsr?.reducedTx)
                assertNotNull(epsr?.message)
            }

            server.shutdown()
        }
    }

    private suspend fun waitWhileFetching(uiLogic: TestErgoPaySigningUiLogic) {
        for (i in 1..50) {
            delay(100)
            if (uiLogic.state != ErgoPaySigningUiLogic.State.FETCH_DATA) break
        }
    }

    private suspend fun buildUiLogic(
        request: String,
        apiFailure: Boolean = false,
        twoAddresses: Boolean = false,
        initWithWallet: Boolean = true
    ): TestErgoPaySigningUiLogic {

        val walletId = 1
        val database =
            if (twoAddresses) TestUiWallet.getSingleWalletTwoAddressesDbProvider(walletId)
            else TestUiWallet.getSingleWalletSingleAddressDbProvider(walletId)

        val uiLogic = TestErgoPaySigningUiLogic(database)
        uiLogic.ergoApiFailure = apiFailure

        val prefs = TestPreferencesProvider()
        uiLogic.init(
            request, if (initWithWallet) walletId else -1, -1,
            database,
            prefs, TestStringProvider()
        )

        return uiLogic
    }

    class TestErgoPaySigningUiLogic(val db: IAppDatabase) : ErgoPaySigningUiLogic() {
        var ergoApiFailure = false

        override val coroutineScope: CoroutineScope
            get() = CoroutineScope(EmptyCoroutineContext)

        override fun notifyStateChanged(newState: State) {

        }

        override fun notifyWalletStateLoaded() {

        }

        override fun notifyDerivedAddressChanged() {

        }

        override fun notifyUiLocked(locked: Boolean) {

        }

        override fun notifyHasErgoTxResult(txResult: TransactionResult) {

        }

        override fun notifyHasSigningPromptData(signingPrompt: String) {

        }

        override fun getErgoApiService(prefs: PreferencesProvider): ApiServiceManager {
            val apiService: ApiServiceManager = mock(ApiServiceManager::class.java)
            whenever(apiService.getExplorerBoxInformation(any())).doAnswer {
                val boxId = it.arguments[0] as String
                return@doAnswer object : Call<OutputInfo> {
                    override fun clone(): Call<OutputInfo> {
                        error("Not implemented")
                    }

                    override fun execute(): Response<OutputInfo> {
                        return if (ergoApiFailure)
                            Response.error(404, ResponseBody.create(null, "Moved"))
                        else
                            Response.success(OutputInfo().apply {
                                setBoxId(boxId)
                                address = wallet?.walletConfig?.firstAddress ?: ""
                                value = 1000L * 1000L * 1000L
                                assets = emptyList()
                            })
                    }

                    override fun enqueue(callback: Callback<OutputInfo>) {
                        error("Not implemented")
                    }

                    override fun isExecuted(): Boolean {
                        error("Not implemented")
                    }

                    override fun cancel() {
                        error("Not implemented")
                    }

                    override fun isCanceled(): Boolean {
                        error("Not implemented")
                    }

                    override fun request(): Request {
                        error("Not implemented")
                    }

                }
            }

            return apiService
        }
    }
}