package com.notekeep.local.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

/**
 * A multi-line EditText that never lets its internal text Layout be measured wider than the
 * space its parent actually gave it - specifically to work around an Android text-layout quirk
 * with Arabic (RTL) content: when the last line in the field is a run of many consecutive
 * trailing spaces (no more text after them yet), StaticLayout's bidi-aware width calculation for
 * that still-open line can come out wider than the field's real available width instead of
 * wrapping, letting the line visually spill past the right/left edge of the screen.
 * android:breakStrategy="simple" and android:scrollHorizontally="false" reduce how often this
 * shows up but don't eliminate it, because the root cause is the *desired width* StaticLayout
 * computes for that trailing-space run under an AT_MOST constraint - not the break strategy or
 * the view's own scroll state.
 *
 * The fix is to never give the base implementation the chance to make that miscalculation in the
 * first place: forcing an EXACTLY width spec before calling into super.onMeasure() means
 * TextView builds its internal Layout to a fixed, correct width from the start, rather than
 * computing a "desired width" from the text content and only comparing it against the AT_MOST
 * bound afterwards (which is where the bidi/trailing-space miscalculation actually happens). A
 * final clamp on the reported measured width is kept as a second line of defense.
 */
class WrapSafeEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Force an EXACTLY width constraint before the base implementation builds its internal
        // text Layout. AT_MOST specs (what match_parent inside a wrap_content-height LinearLayout
        // normally resolves to here) let StaticLayout's own width calculation - which is what
        // under-measures a trailing run of Arabic-context spaces - decide the final width instead
        // of the space actually available; EXACTLY removes that discretion entirely, so the
        // internal Layout is always built to fit and any overflowing run is forced to wrap rather
        // than spill past the edge.
        val mode = android.view.View.MeasureSpec.getMode(widthMeasureSpec)
        val size = android.view.View.MeasureSpec.getSize(widthMeasureSpec)
        val exactSpec = if (mode != android.view.View.MeasureSpec.UNSPECIFIED) {
            android.view.View.MeasureSpec.makeMeasureSpec(size, android.view.View.MeasureSpec.EXACTLY)
        } else {
            widthMeasureSpec
        }
        super.onMeasure(exactSpec, heightMeasureSpec)
        // Belt-and-braces: even with an exact spec, clamp the final reported width so this view
        // can never claim more horizontal space than its parent actually gave it.
        if (mode != android.view.View.MeasureSpec.UNSPECIFIED && measuredWidth > size) {
            setMeasuredDimension(size, measuredHeight)
        }
    }
}
