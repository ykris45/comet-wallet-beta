package org.ergoplatform.android.wallet

import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.R
import org.ergoplatform.android.RoomWalletDbProvider
import org.ergoplatform.android.databinding.FragmentSaveWalletDialogBinding
import org.ergoplatform.android.ui.*
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.api.AndroidEncryptionManager
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.persistance.ENC_TYPE_DEVICE
import org.ergoplatform.persistance.ENC_TYPE_PASSWORD

/**
 * Dialog to save a created or restored wallet
 */
class SaveWalletFragmentDialog : FullScreenFragmentDialog(), PasswordDialogCallback {
    private var _binding: FragmentSaveWalletDialogBinding? = null
    private val binding get() = _binding!!

    private val args: SaveWalletFragmentDialogArgs by navArgs()
    private lateinit var viewModel: SaveWalletViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSaveWalletDialogBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(SaveWalletViewModel::class.java)

        // TODO avoid using mnemonic here, store and use publicAddress directly
        // Reason why this is not done: mnemonic is stored in arguments to allow Android to
        // destroy and recreate this dialog without the loss of the mnemonic. Possible
        // workarounds not implemented because of their drawbacks:
        // - Storing it as a SecretString in a shared ViewModel for the wallet creation dialogs
        //   Drawback: The ViewModel is reset when the destruction of the dialog is done due to low
        //             memory, hence we could lose the mnemonic on low end devices
        // - Use a static variable to store the mnemonic in a SecretString
        //   Drawback: It is completely out of control when static variables get reset and the
        //             variable might leak into a process reusing the JVM
        viewModel.init(SecretString.create(args.mnemonic), args.fromRestore, requireContext())

        viewModel.publicAddress.observe(viewLifecycleOwner) {
            val context = requireContext()
            val db = RoomWalletDbProvider(AppDatabase.getInstance(context))
            val texts = AndroidStringProvider(context)

            val uiLogic = viewModel.uiLogic!!
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val publicErgoAddressFromMnemonic = uiLogic.publicAddress
                val walletDisplayName = uiLogic.getSuggestedDisplayName(db, texts)
                val showDisplayName = uiLogic.showSuggestedDisplayName(db)

                withContext(Dispatchers.Main) {
                    binding.publicAddress.text = publicErgoAddressFromMnemonic
                    binding.cardViewContainer.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                    binding.inputWalletName.editText?.setText(walletDisplayName)
                    binding.inputWalletName.visibility =
                        if (showDisplayName) View.VISIBLE else View.GONE
                    binding.buttonAltAddress.visibility =
                        if (uiLogic.hasAlternativeAddress) View.VISIBLE else View.GONE
                }
            }

            viewModel.derivedAddressNum.observe(viewLifecycleOwner) { addrNum ->
                binding.introAdditionalAddresses.text =
                    if (addrNum == 0) getString(R.string.intro_save_wallet2) else getString(
                        R.string.intro_save_wallet_derived_addresses_num,
                        (addrNum + 1).toString()
                    )
            }
        }

        val context = requireContext()
        val bmm = BiometricManager.from(context)
        val methodDesc =
            if (bmm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS)
                R.string.device_enc_security_biometric_strong
            else if (bmm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS)
                R.string.device_enc_security_biometric_weak
            else if ((context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure)
                R.string.device_enc_security_pass
            else R.string.device_enc_security_none

        binding.descDeviceEncryption.text =
            getString(R.string.desc_save_device_encrypted, getString(methodDesc))

        if (methodDesc == R.string.device_enc_security_none) {
            binding.buttonSaveDeviceenc.isEnabled = false
        }

        binding.buttonSavePassenc.setOnClickListener {
            val passwordDialogFragment = PasswordDialogFragment()
            val args = Bundle()
            args.putBoolean(ARG_SHOW_CONFIRMATION, true)
            passwordDialogFragment.arguments = args
            passwordDialogFragment.show(
                childFragmentManager,
                null
            )
        }
        binding.buttonSaveDeviceenc.setOnClickListener {
            showBiometricPrompt()
        }
        binding.buttonAltAddress.setOnClickListener {
            viewModel.switchAddress(requireContext())
        }
    }

    private fun showBiometricPrompt() {

        // setDeviceCredentialAllowed is deprecated, but needed for older SDK level
        @Suppress("DEPRECATION") val promptInfo = PromptInfo.Builder()
            .setTitle(getString(R.string.title_authenticate))
            .setConfirmationRequired(false)
            .setDeviceCredentialAllowed(true)
            .build()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                try {
                    val secretStorage = AndroidEncryptionManager.encryptDataOnDevice(
                        viewModel.uiLogic!!.signingSecrets.toBytes()
                    )
                    saveToDbAndNavigateToWallet(
                        ENC_TYPE_DEVICE, secretStorage
                    )

                } catch (t: Throwable) {
                    showSecurityErrorSnackbar(t)
                }
            }
        }

        try {
            BiometricPrompt(this, callback).authenticate(promptInfo)
        } catch (t: Throwable) {
            showSecurityErrorSnackbar(t)
        }
    }

    private fun showSecurityErrorSnackbar(t: Throwable) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.error_device_security_save_wallet, t.message))
            .setPositiveButton(R.string.zxing_button_ok, null)
            .show()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveToDbAndNavigateToWallet(encType: Int, secretStorage: ByteArray) {

        val context = requireContext()
        GlobalScope.launch(Dispatchers.IO) {
            // make sure not to use dialog context within this block
            viewModel.uiLogic!!.suspendSaveToDb(
                RoomWalletDbProvider(AppDatabase.getInstance(context)),
                binding.inputWalletName.editText!!.text.toString(),
                encType,
                secretStorage
            )
        }
        NavHostFragment.findNavController(requireParentFragment())
            .navigateSafe(SaveWalletFragmentDialogDirections.actionSaveWalletFragmentDialogToNavigationWallet())
    }

    override fun onPasswordEntered(password: SecretString?): String? {
        return if (viewModel.uiLogic!!.isPasswordWeak(password)) {
            getString(R.string.err_password)
        } else {
            saveToDbAndNavigateToWallet(
                ENC_TYPE_PASSWORD,
                AesEncryptionManager.encryptData(
                    password,
                    viewModel.uiLogic!!.signingSecrets.toBytes()
                )
            )
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

