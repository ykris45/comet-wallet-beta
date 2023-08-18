package org.ergoplatform.uilogic

import org.ergoplatform.mosaik.MosaikDbProvider
import org.ergoplatform.persistance.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

object TestUiWallet {
    const val firstAddress = "main_address"
    const val secondAddress = "second_address"
    internal val token = WalletToken(
        1,
        firstAddress,
        firstAddress,
        "74251ce2cb4eb2024a1a155e19ad1d1f58ff8b9e6eb034a3bb1fd58802757d23",
        10,
        0,
        "testtoken"
    )
    internal val singularToken = WalletToken(
        2,
        firstAddress,
        firstAddress,
        "ba5856162d6342d2a0072f464a5a8b62b4ac4dd77195bec18c6bf268c2def831",
        1,
        0,
        "nft"
    )
    internal val singleAddressWallet = Wallet(
        WalletConfig(1, "test", firstAddress, 0, null, false, null),
        listOf(WalletState(firstAddress, firstAddress, 1000L * 1000 * 1000, 0)),
        listOf(token, singularToken),
        emptyList()
    )

    private val firstDerivedAddress = WalletAddress(1, firstAddress, 1, secondAddress, null)
    internal val twoAddressesWallet = Wallet(
        WalletConfig(1, "test", firstAddress, 0, null, false, null),
        listOf(WalletState(firstAddress, firstAddress, 1000L * 1000 * 1000, 0)),
        listOf(token, singularToken),
        listOf(firstDerivedAddress)
    )

    suspend fun getSingleWalletSingleAddressDbProvider(walletId: Int): IAppDatabase {
        val walletDbProvider = mock<WalletDbProvider> {
        }
        whenever(walletDbProvider.loadWalletWithStateById(walletId)).thenReturn(singleAddressWallet)
        //whenever(walletDbProvider.getAllWalletConfigsSynchronous()).thenReturn(listOf(
        //singleAddressWallet.walletConfig)) <- this will break ErgoPaySigningUiLogicTest because it relies on
        // this not having implemented. If it is needed, test will fail expectedly
        whenever(walletDbProvider.loadWalletByFirstAddress(firstAddress)).thenReturn(
            singleAddressWallet.walletConfig
        )
        return object : IAppDatabase {
            override val walletDbProvider: WalletDbProvider
                get() = walletDbProvider
            override val tokenDbProvider: TokenDbProvider
                get() = mock {}
            override val transactionDbProvider: TransactionDbProvider
                get() = mock {}
            override val mosaikDbProvider: MosaikDbProvider
                get() = mock {}
            override val addressBookDbProvider: AddressBookDbProvider
                get() = mock {}
        }
    }

    suspend fun getSingleWalletTwoAddressesDbProvider(walletId: Int): IAppDatabase {
        val walletDbProvider = mock<WalletDbProvider> {
        }
        whenever(walletDbProvider.loadWalletWithStateById(walletId)).thenReturn(twoAddressesWallet)
        whenever(walletDbProvider.loadWalletByFirstAddress(firstAddress)).thenReturn(
            twoAddressesWallet.walletConfig
        )
        return object : IAppDatabase {
            override val walletDbProvider: WalletDbProvider
                get() = walletDbProvider
            override val tokenDbProvider: TokenDbProvider
                get() = mock {}
            override val transactionDbProvider: TransactionDbProvider
                get() = mock {}
            override val mosaikDbProvider: MosaikDbProvider
                get() = mock {}
            override val addressBookDbProvider: AddressBookDbProvider
                get() = mock {}
        }
    }
}