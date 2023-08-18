package org.ergoplatform.ios.transactions

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.ErgoAmount
import org.ergoplatform.TokenAmount
import org.ergoplatform.explorer.client.model.AdditionalRegisters
import org.ergoplatform.explorer.client.model.AssetInstanceInfo
import org.ergoplatform.ios.tokens.TokenEntryView
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.STRING_LABEL_ERG_AMOUNT
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class TransactionBoxEntryView(private val vc: UIViewController) : UIView(CGRect.Zero()) {
    private var address: String? = null

    private val labelBoxAddress = Body1BoldLabel().apply {
        numberOfLines = 1
        textColor = uiColorErgo
        lineBreakMode = NSLineBreakMode.TruncatingMiddle
        isUserInteractionEnabled = true
        addGestureRecognizer(UITapGestureRecognizer {
            numberOfLines = if (numberOfLines == 1L) 10 else 1
        })
        addGestureRecognizer(UILongPressGestureRecognizer {
            if (it.state == UIGestureRecognizerState.Began) {
                HapticFeedback().perform(UIImpactFeedbackStyle.Medium)
                address?.let { vc.shareText(it, this) }
            }
        })
    }
    private val labelErgAmount = Body1BoldLabel().apply {
        numberOfLines = 1
    }
    private val tokensList = UIStackView().apply {
        axis = UILayoutConstraintAxis.Vertical
    }
    private val registerList = UIStackView().apply {
        axis = UILayoutConstraintAxis.Vertical
    }

    init {
        layoutMargins = UIEdgeInsets.Zero()

        addSubview(labelBoxAddress)
        addSubview(labelErgAmount)
        addSubview(tokensList)
        addSubview(registerList)

        labelBoxAddress.topToSuperview(topInset = DEFAULT_MARGIN).widthMatchesSuperview(inset = DEFAULT_MARGIN)
        labelErgAmount.topToBottomOf(labelBoxAddress).widthMatchesSuperview(inset = DEFAULT_MARGIN * 2)
        tokensList.topToBottomOf(labelErgAmount).widthMatchesSuperview(inset = DEFAULT_MARGIN * 2)
        registerList.topToBottomOf(tokensList).widthMatchesSuperview(inset = DEFAULT_MARGIN * 2).bottomToSuperview()
    }

    fun bindBoxView(
        value: Long?,
        address: String,
        assets: List<AssetInstanceInfo>?,
        registers: AdditionalRegisters?,
        tokenClickListener: ((String) -> Unit)?,
        addressLabelHandler: ((String, (String) -> Unit) -> Unit)?,
        tokenLabelHandler: ((String, (String) -> Unit) -> Unit)?,
        texts: I18NBundle
    ): TransactionBoxEntryView {
        labelErgAmount.text = value?.let {
            if (it != 0L) texts.format(
                STRING_LABEL_ERG_AMOUNT,
                ErgoAmount(value).toStringTrimTrailingZeros()
            ) else null
        } ?: ""
        this.address = address
        labelBoxAddress.text = address

        tokensList.apply {
            clearArrangedSubviews()

            assets?.forEach { token ->
                addArrangedSubview(TokenEntryView().apply {
                    // we use the token id here, we don't have the name in the cold wallet context
                    bindWalletToken(
                        token.name ?: token.tokenId,
                        TokenAmount(token.amount, token.decimals ?: 0).toStringUsFormatted()
                    )
                    tokenClickListener?.let {
                        addGestureRecognizer(UITapGestureRecognizer { tokenClickListener(token.tokenId) })
                    }
                    if (token.name == null && tokenLabelHandler != null) {
                        tokenLabelHandler.invoke(token.tokenId) { displayName -> setTokenName(displayName) }
                    }
                })
            }
        }

        registerList.apply {
            clearArrangedSubviews()
            registers?.entries?.sortedBy { it.key }?.forEach {
                addArrangedSubview(Body2Label().apply {
                    text = it.key + ": " + it.value.sigmaType + ", " + it.value.renderedValue
                    numberOfLines = 3
                    lineBreakMode = NSLineBreakMode.TruncatingTail
                })
            }
        }

        addressLabelHandler?.invoke(address) { label ->
            labelBoxAddress.text = label
            labelBoxAddress.lineBreakMode = NSLineBreakMode.TruncatingTail
        }

        return this
    }
}