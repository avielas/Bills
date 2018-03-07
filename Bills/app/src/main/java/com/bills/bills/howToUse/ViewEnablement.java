package com.bills.bills.howToUse;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created by aviel on 3/4/18.
 */

public class ViewEnablement {
    public static void EnableView(ViewGroup layout){
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof ViewGroup) {
                EnableView((ViewGroup) child);
            } else {
                child.setEnabled(true);
            }
        }
    }

    public static void DisableView(ViewGroup layout){
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof ViewGroup) {
                DisableView((ViewGroup) child);
            } else {
                child.setEnabled(false);
            }
        }
    }
}
