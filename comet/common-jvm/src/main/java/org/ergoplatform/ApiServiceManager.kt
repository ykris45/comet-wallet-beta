package org.ergoplatform

import org.ergoplatform.api.*
import org.ergoplatform.appkit.Address
import org.ergoplatform.explorer.client.DefaultApi
import org.ergoplatform.explorer.client.model.*
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.restapi.client.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import scorex.util.encode.Base16

open class ApiServiceManager(
    private val defaultApi: DefaultApi,
    private val nodeApiUrl: String,
    val preferNodeAsExplorer: Boolean,
    private val tokenVerificationApi: TokenVerificationApi
) : ErgoExplorerApi, TokenVerificationApi, ErgoNodeApi {

    private val nodeTransactionsApi by lazy {
        buildRetrofitForNode(TransactionsApi::class.java, nodeApiUrl)
    }
    private val nodeBoxesApi by lazy {
        buildRetrofitForNode(UtxoApi::class.java, nodeApiUrl)
    }
    private val nodeBlockchainApi by lazy {
        buildRetrofitForNode(BlockchainApi::class.java, nodeApiUrl)
    }

    fun getTotalBalanceForAddress(publicAddress: String, useNode: Boolean): Call<TotalBalance> =
        if (useNode) nodeBlockchainApi.getBalance(publicAddress)
        else defaultApi.getApiV1AddressesP1BalanceTotal(publicAddress)

    override fun getNodeBoxInformation(boxId: String): Call<ErgoTransactionOutput> =
        nodeBlockchainApi.getBoxById(boxId)

    override fun getExplorerBoxInformation(boxId: String): Call<OutputInfo> =
        defaultApi.getApiV1BoxesP1(boxId)

    override fun getNodeUnspentBoxInformation(boxId: String): Call<ErgoTransactionOutput> =
        nodeBoxesApi.getBoxWithPoolById(boxId)

    override fun getTokenInfoNode(tokenId: String): Call<BlockchainToken> =
        nodeBlockchainApi.getTokenById(tokenId)

    override fun getTokenInformation(tokenId: String): Call<TokenInfo> =
        defaultApi.getApiV1TokensP1(tokenId)

    override fun getTransactionInformation(txId: String): Call<TransactionInfo> =
        defaultApi.getApiV1TransactionsP1(txId)

    override fun getTransactionInformationNode(txId: String): Call<BlockchainTransaction> =
        nodeBlockchainApi.getTxById(txId)

    override fun getTransactionInformationUncomfirmedNode(txId: String): Call<ErgoTransaction> =
        nodeTransactionsApi.getUnconfirmedTransactionById(txId)

    // this is the Ergo Explorer call
    override fun getMempoolTransactionsForAddress(
        publicAddress: String,
        limit: Int,
        offset: Int
    ): Call<Items<TransactionInfo>> =
        defaultApi.getApiV1MempoolTransactionsByaddressP1(publicAddress, offset, limit)

    // this is the Node API call
    override fun getNodeMempoolTransactionsForAddress(
        publicAddress: String,
        limit: Int
    ): Call<Transactions> =
        nodeTransactionsApi.getUnconfirmedTransactionsByErgoTree(
            "\"" + Base16.encode(Address.create(publicAddress).toPropositionBytes()) + "\"",
            0, limit
        )

    // this is the node API call for all addresses
    override fun getUnconfirmedTransactions(limit: Int): Call<Transactions> =
        nodeTransactionsApi.getUnconfirmedTransactions(limit, 0)

    override fun getExpectedWaitTime(fee: Long, txSize: Int): Call<Long> =
        nodeTransactionsApi.getExpectedWaitTime(fee.toInt(), txSize)

    override fun getSuggestedFee(waitTime: Int, txSize: Int): Call<Int> =
        nodeTransactionsApi.getRecommendedFee(waitTime, txSize)

    override fun getConfirmedTransactionsForAddress(
        publicAddress: String,
        limit: Int,
        offset: Int
    ): Call<Items<TransactionInfo>> =
        // TODO concise should be true when https://github.com/ergoplatform/explorer-backend/issues/193 is fixed
        defaultApi.getApiV1AddressesP1Transactions(publicAddress, offset, limit, false)

    override fun getNodeConfirmedTransactionsForAddress(
        publicAddress: String,
        limit: Int,
        offset: Int
    ): Call<Items<BlockchainTransaction>> =
        nodeBlockchainApi.getTransactionsByAddress(publicAddress, limit, offset)

    override fun checkToken(tokenId: String, tokenName: String): Call<TokenCheckResponse> =
        tokenVerificationApi.checkToken(tokenId, tokenName.replace("/", "-").replace("|", "-"))

    companion object {
        private var ergoApiService: ApiServiceManager? = null

        fun <S> buildRetrofitForNode(serviceClass: Class<S>, nodeApiUrl: String): S {
            val retrofitNode = Retrofit.Builder()
                .baseUrl(nodeApiUrl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(OkHttpSingleton.getInstance())
                .build()
            return retrofitNode.create(serviceClass)
        }

        fun getOrInit(preferences: PreferencesProvider): ApiServiceManager {
            if (ergoApiService == null) {

                val retrofitExplorer = Retrofit.Builder()
                    .baseUrl(preferences.prefExplorerApiUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(OkHttpSingleton.getInstance())
                    .build()
                val defaultApi = retrofitExplorer.create(DefaultApi::class.java)

                val retrofitTokenVerify = Retrofit.Builder()
                    .baseUrl(preferences.prefTokenVerificationUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(OkHttpSingleton.getInstance())
                    .build()
                val tokenVerificationApi =
                    retrofitTokenVerify.create(TokenVerificationApi::class.java)

                ergoApiService = ApiServiceManager(
                    defaultApi,
                    preferences.prefNodeUrl,
                    preferences.isPreferNodeExplorer,
                    tokenVerificationApi
                )
            }
            return ergoApiService!!
        }


        fun resetApiService() {
            ergoApiService = null
        }


    }
}