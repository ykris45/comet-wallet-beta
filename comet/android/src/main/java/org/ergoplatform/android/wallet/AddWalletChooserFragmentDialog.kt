package org.ergoplatform.android.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment
import org.ergoplatform.android.databinding.FragmentAddWalletChooserBinding
import org.ergoplatform.android.ui.FullScreenFragmentDialog
import org.ergoplatform.android.ui.navigateSafe

/**
 * Add wallet step 1: Chooser to import or add a new fragment
 */
class AddWalletChooserFragmentDialog : FullScreenFragmentDialog() {

    private var _binding: FragmentAddWalletChooserBinding? = null

    // This property is only valid between onCreateDialog and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddWalletChooserBinding.inflate(inflater, container, false)

        binding.cardCreateWallet.setOnClickListener {
            NavHostFragment.findNavController(requireParentFragment())
                .navigateSafe(AddWalletChooserFragmentDialogDirections.actionToCreateWalletDialog())
        }

        binding.cardRestoreWallet.setOnClickListener {
            NavHostFragment.findNavController(requireParentFragment())
                .navigateSafe(AddWalletChooserFragmentDialogDirections.actionToRestoreWalletFragmentDialog())
        }

        binding.cardReadonlyWallet.setOnClickListener {
            NavHostFragment.findNavController(requireParentFragment())
                .navigateSafe(AddWalletChooserFragmentDialogDirections.actionAddWalletChooserFragmentToAddReadOnlyWalletFragmentDialog())
        }

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}