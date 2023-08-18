package org.ergoplatform.api.ergodex

import org.ergoplatform.api.OkHttpSingleton
import org.ergoplatform.api.PriceImportance
import org.ergoplatform.api.TokenPriceApi
import org.ergoplatform.persistance.TokenPrice
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ErgoDexPriceApi : TokenPriceApi {
    private val priceSource = "spectrum.fi"
    private val baseIdErg = "0000000000000000000000000000000000000000000000000000000000000000"

    private val ergoDexApi: ErgoDexApi

    init {
        val retrofit = Retrofit.Builder().baseUrl("https://api.spectrum.fi/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpSingleton.getInstance())
            .build()
        ergoDexApi = retrofit.create(ErgoDexApi::class.java)
    }


    override fun getTokenPrices(): List<Pair<TokenPrice, PriceImportance>>? {
        val swapList = ergoDexApi.swaps.execute()

        val ergBasePrices = swapList.body()
            ?.filter { it.baseId.equals(baseIdErg) && it.baseDisplayName.equals("ERG") }

        return ergBasePrices?.let {

            val hashMap = HashMap<String, ErgoDexSwap>()
            ergBasePrices.forEach {
                val otherEntry = hashMap[it.tokenId]

                // prefer pools without more trading volume
                if (otherEntry == null || otherEntry.baseVolume.value < it.baseVolume.value)
                    hashMap[it.tokenId] = it
            }

            hashMap.values.map {
                TokenPrice(it.tokenId, it.displayName, priceSource, it.lastPrice)
            }.map { Pair(it, PriceImportance.Low) }
        }
    }
}