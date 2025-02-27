package com.bills.bills.howToUse;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.bills.bills.R;
import com.bills.billslib.Contracts.Constants;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

/**
 * Created by aviel on 2/24/18.
 */

public class HowToUseWelcomeScreen {
    private final String PREFS_SHOWCASE_INTERNAL = "showcase_internal";
    private Target mViewTarget;
    private Activity mActivity;
    private ShowcaseView mShowcaseView;
    private Integer mCounter = 0;
    private ViewGroup mWorkOnView;

    public HowToUseWelcomeScreen(Activity activity, ViewGroup workOnView){
        mActivity = activity;
        mWorkOnView = workOnView;
    }

    public void SetShowcaseViewStartCamera() {
        if(mActivity
                .getSharedPreferences(PREFS_SHOWCASE_INTERNAL, Context.MODE_PRIVATE)
                .getBoolean("hasShot" + Constants.SHOT_ID_WELCOME_SCREEN, false)){
            return;
        }
        ViewEnablement.DisableView(mWorkOnView);
        mViewTarget = new ViewTarget(R.id.start_camera_button, mActivity);
        mShowcaseView = new ShowcaseView.Builder(mActivity)
                .setTarget(mViewTarget)
                .setOnClickListener(onClickListener)
                .setContentTitle(R.string.start_camera_button_title)
                .setStyle(R.style.CustomShowcaseTheme)
                .setContentText(R.string.start_camera_button_description)
                .singleShot(Constants.SHOT_ID_WELCOME_SCREEN)
                .build();
        mShowcaseView.setButtonText(mActivity.getString(R.string.next_desc));


        //set margin of next_desc button
        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lps.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        lps.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        int margin = ((Number) (mActivity.getResources().getDisplayMetrics().density * 18)).intValue();
        lps.setMargins(margin, margin, margin, margin);
        mShowcaseView.setButtonPosition(lps);
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (mCounter) {
                case 0:
                    mViewTarget = new ViewTarget(R.id.check_pass_code_button, mActivity);
                    mShowcaseView.setShowcase(mViewTarget, true);
                    mShowcaseView.setContentTitle(mActivity.getString(R.string.check_pass_code_button_title));
                    mShowcaseView.setContentText(mActivity.getString(R.string.check_pass_code_button_description));
                    mShowcaseView.setButtonText(mActivity.getString(R.string.close));
                    break;
                case 1:
                    mShowcaseView.hide();
                    ViewEnablement.EnableView(mWorkOnView);
                    break;
            }
            mCounter++;
        }
    };
}
