package org.ergoplatform.ios.wallet

import org.ergoplatform.appkit.SecretString
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.STRING_CHECK_CONFIRM_CREATE_WALLET
import org.ergoplatform.uilogic.STRING_INTRO_CONFIRM_CREATE_WALLET
import org.ergoplatform.uilogic.STRING_LABEL_CREATE_WALLET
import org.ergoplatform.uilogic.STRING_LABEL_WORD_CONFIRM_CREATE_WALLET
import org.ergoplatform.uilogic.wallet.ConfirmCreateWalletUiLogic
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class ConfirmCreateWalletViewController(val mnemonic: SecretString) : ViewControllerWithKeyboardLayoutGuide() {
    private val uiLogic = ConfirmCreateWalletUiLogic()

    override fun viewDidLoad() {
        super.viewDidLoad()

        val texts = getAppDelegate().texts
        title = texts.get(STRING_LABEL_CREATE_WALLET)
        view.backgroundColor = UIColor.systemBackground()

        if (navigationController.viewControllers.size == 1) {
            val cancelButton = UIBarButtonItem(UIBarButtonSystemItem.Cancel)
            cancelButton.setOnClickListener { this.dismissViewController(true) {} }
            navigationItem.leftBarButtonItem = cancelButton
        }

        navigationController.navigationBar?.tintColor = uiColorErgo

        val nextButton = UIBarButtonItem(UIBarButtonSystemItem.Done)
        navigationItem.rightBarButtonItem = nextButton

        uiLogic.mnemonic = mnemonic

        val container = UIView()
        val scrollView = container.wrapInVerticalScrollView()
        view.addSubview(scrollView)
        scrollView.widthMatchesSuperview().topToSuperview().bottomToKeyboard(this)

        val introLabel = Body1Label()
        introLabel.text = texts.get(STRING_INTRO_CONFIRM_CREATE_WALLET)

        val textField1 = EndIconTextField()
        val textField2 = EndIconTextField()
        textField1.placeholder = texts.format(STRING_LABEL_WORD_CONFIRM_CREATE_WALLET, uiLogic.firstWord.toString())
        textField1.autocapitalizationType = UITextAutocapitalizationType.None
        textField1.returnKeyType = UIReturnKeyType.Next
        textField1.delegate = object : UITextFieldDelegateAdapter() {
            override fun shouldReturn(textField: UITextField?): Boolean {
                textField2.becomeFirstResponder()
                return super.shouldReturn(textField)
            }
        }
        textField2.placeholder = texts.format(STRING_LABEL_WORD_CONFIRM_CREATE_WALLET, uiLogic.secondWord.toString())
        textField2.autocapitalizationType = UITextAutocapitalizationType.None
        textField2.returnKeyType = UIReturnKeyType.Next
        textField2.delegate = object : UITextFieldDelegateAdapter() {
            override fun shouldReturn(textField: UITextField?): Boolean {
                textField2.resignFirstResponder()
                return super.shouldReturn(textField)
            }
        }

        val confirmationContainer = UiSwitchWithLabel(texts.get(STRING_CHECK_CONFIRM_CREATE_WALLET))
        val confirmationCheck = confirmationContainer.switch

        val verticalStack = UIStackView(
            NSArray(
                introLabel,
                textField1,
                textField2,
                confirmationContainer
            )
        )
        verticalStack.axis = UILayoutConstraintAxis.Vertical
        verticalStack.spacing = DEFAULT_MARGIN * 2
        container.addSubview(verticalStack)
        verticalStack.topToSuperview(false, DEFAULT_MARGIN * 3)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH).bottomToSuperview()

        container.addSubview(verticalStack)

        nextButton.setOnClickListener {
            val hasErrors = uiLogic.checkUserEnteredCorrectWordsAndConfirmedObligations(textField1.text, textField2.text, confirmationCheck.isOn)

            textField1.setHasError(!uiLogic.firstWordCorrect)
            textField2.setHasError(!uiLogic.secondWordCorrect)

            if (!hasErrors) {
                navigationController.pushViewController(
                    SaveWalletViewController(mnemonic, false),
                    true
                )
            }
        }

    }
}