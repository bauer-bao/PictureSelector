package com.luck.picture.lib.tools;

import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.widget.TextView;

import com.luck.picture.lib.R;

/**
 * project：PictureSelector
 * package：com.luck.picture.lib.tool
 * email：893855882@qq.com
 * data：2017/5/25
 *
 * @author luck
 */
public class StringUtils {
    public static void tempTextFont(TextView tv) {
        String text = tv.getText().toString().trim();
        String str = tv.getContext().getString(R.string.picture_empty_title);
        String sumText = str + text;
        Spannable placeSpan = new SpannableString(sumText);
        placeSpan.setSpan(new RelativeSizeSpan(0.8f), str.length(), sumText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(placeSpan);
    }

    public static void modifyTextViewDrawable(TextView v, Drawable drawable, int index) {
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        //index 0:左 1：上 2：右 3：下
        if (index == 0) {
            v.setCompoundDrawables(drawable, null, null, null);
        } else if (index == 1) {
            v.setCompoundDrawables(null, drawable, null, null);
        } else if (index == 2) {
            v.setCompoundDrawables(null, null, drawable, null);
        } else {
            v.setCompoundDrawables(null, null, null, drawable);
        }
    }

}
