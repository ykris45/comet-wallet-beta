package org.ergoplatform.ios.wallet

import org.ergoplatform.appkit.SecretString
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.STRING_INTRO_RESTORE_WALLET
import org.ergoplatform.uilogic.STRING_LABEL_RESTORE_WALLET
import org.ergoplatform.uilogic.STRING_LABEL_RESTORE_WALLET_WORD_LIST
import org.ergoplatform.uilogic.wallet.RestoreWalletUiLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSRange
import org.robovm.apple.uikit.*

class RestoreWalletViewController : ViewControllerWithKeyboardLayoutGuide() {
    lateinit var tvMnemonic: UITextView
    lateinit var errorLabel: Body1Label
    lateinit var wordListHint: UITextView

    override fun viewDidLoad() {
        super.viewDidLoad()

        val texts = getAppDelegate().texts
        title = texts.get(STRING_LABEL_RESTORE_WALLET)
        view.backgroundColor = UIColor.systemBackground()
        val uiLogic = IosRestoreWalletUiLogic(IosStringProvider(texts))

        if (navigationController.viewControllers.size == 1) {
            val cancelButton = UIBarButtonItem(UIBarButtonSystemItem.Cancel)
            cancelButton.setOnClickListener { this.dismissViewController(true) {} }
            navigationItem.leftBarButtonItem = cancelButton
        }

        navigationController.navigationBar?.tintColor = uiColorErgo

        val nextButton = UIBarButtonItem(UIBarButtonSystemItem.Done)
        navigationItem.rightBarButtonItem = nextButton
        nextButton.setOnClickListener { uiLogic.doRestore() }

        val descLabel = Body1Label()
        descLabel.text = texts.get(STRING_INTRO_RESTORE_WALLET)
        tvMnemonic = createTextview()
        tvMnemonic.textContentType = UITextContentType.OneTimeCode
        tvMnemonic.isSecureTextEntry = true
        tvMnemonic.returnKeyType = UIReturnKeyType.Done

        wordListHint = UITextView(CGRect.Zero())

        errorLabel = Body1Label()
        errorLabel.textColor = UIColor.systemRed()

        tvMnemonic.setDelegate(object : UITextViewDelegateAdapter() {
            override fun shouldChangeCharacters(
                textView: UITextView?,
                range: NSRange?,
                text: String?
            ): Boolean {
                if (text == "\n") {
                    uiLogic.doRestore()
                    return false
                }
                return true
            }

            override fun didChange(textView: UITextView?) {
                uiLogic.userChangedMnemonic()
            }
        })

        container.addSubviews(listOf(descLabel, wordListHint, tvMnemonic, errorLabel))

        descLabel.widthMatchesSuperview(false, DEFAULT_MARGIN).topToSuperview(false, DEFAULT_MARGIN)
        wordListHint.widthMatchesSuperview(false, DEFAULT_MARGIN)
            .topToBottomOf(descLabel, DEFAULT_MARGIN)
        // make the word list get compressed on iPhone SE
        wordListHint.setContentCompressionResistancePriority(500f, UILayoutConstraintAxis.Vertical)
        tvMnemonic.widthMatchesSuperview(false, DEFAULT_MARGIN)
            .topToBottomOf(wordListHint, DEFAULT_MARGIN * 2)
        errorLabel.widthMatchesSuperview().topToBottomOf(tvMnemonic)
            .bottomToKeyboard(this, DEFAULT_MARGIN)

    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        // for some reason, setting setHtmlText in viewDidLoad does not work when this VC is
        // invoked from empty cell. Moving it to here works.
        wordListHint.apply {
            setHtmlText(getAppDelegate().texts.get(STRING_LABEL_RESTORE_WALLET_WORD_LIST))
            font = UIFont.getSystemFont(FONT_SIZE_BODY1, UIFontWeight.Regular)
        }
    }

    inner class IosRestoreWalletUiLogic(stringProvider: IosStringProvider) :
        RestoreWalletUiLogic(stringProvider) {

        override fun getEnteredMnemonic(): CharSequence? = tvMnemonic.text

        override fun setErrorLabel(error: String?) {
            errorLabel.text = error ?: ""
            tvMnemonic.setHasError(!error.isNullOrEmpty())
        }

        override fun navigateToSaveWalletDialog(mnemonic: String) {
            navigationController.pushViewController(
                SaveWalletViewController(
                    SecretString.create(
                        mnemonic
                    ), true
                ), true
            )
        }

        override fun hideForcedSoftKeyboard() {
            // not needed, iOS layout is different from Android
        }

    }
}