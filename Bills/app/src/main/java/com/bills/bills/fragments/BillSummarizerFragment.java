package com.bills.bills.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bills.bills.R;
import com.bills.bills.firebase.UiUpdater;
import com.bills.bills.howToUse.HowToUseBillSummarizer;
import com.bills.billslib.Contracts.BillRow;
import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Enums.LogsDestination;
import com.bills.billslib.Core.BillsLog;

import java.util.List;
import java.util.UUID;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BillSummarizerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class BillSummarizerFragment extends Fragment implements HowToUseBillSummarizer.onFinishedListener {
    private static String Tag = BillSummarizerFragment.class.getName();
    private int mPassCode;
    private String mDbPath;
    private String mStoragePath;
    private OnFragmentInteractionListener mListener;
    private UUID mSessionId;

    private boolean mMainUserMode;
    private Context mContext;

    private UiUpdater mUiUpdater;

    private LinearLayout mCommonItemsArea;
    private LinearLayout mMyItemsArea;
    private TextView mCommonTotalSumView;
    private TextView mMyTotalSumView;
    private EditText mTipPercentView;
    private EditText mTipSumView;
    private TextView mPassCodeView;
    private TextView mCommonItemsCount;
    private TextView mMyItemsCount;

    private List<BillRow> mBillRows;
    private ImageView mScreenSpliter;

    private int mScreenWidth;
    public BillSummarizerFragment() {
        // Required empty public constructor
    }

    //Secondary user
    public void Init(final UUID sessionId, Context context, Integer passCode, String dbPath, String storagePath) {
        mPassCode = passCode;
        mDbPath = dbPath;
        mStoragePath = storagePath;
        mContext = context;
        mMainUserMode = false;
        mSessionId = sessionId;
    }

    public void Init(UUID sessionId, Context context, Integer passCode, String dbPath, List<BillRow> rows, int screenWidth) {
        mPassCode = passCode;
        mDbPath = dbPath;
        mContext = context;
        mBillRows = rows;
        mMainUserMode = true;
        mSessionId = sessionId;
        mScreenWidth = screenWidth;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        HowToUseBillSummarizer howToUseBillSummarizer = new HowToUseBillSummarizer(getActivity(), this,
                (ViewGroup) getActivity().findViewById(R.id.bill_summarizer_frame_layout));
        howToUseBillSummarizer.SetShowcaseViewBillSummarizer();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bill_summarizer, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            BillsLog.Log(mSessionId, LogLevel.Error, context.toString() + " must implement OnCameraFragmentInteractionListener", LogsDestination.BothUsers, Tag);
            throw new RuntimeException(context.toString() + " must implement OnCameraFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mBillRows = null;
        mListener = null;
    }

    @Override
    public void finished() {

        mCommonItemsArea = (LinearLayout)getView().findViewById(R.id.common_items_area_linearlayout);
        mMyItemsArea = (LinearLayout)getView().findViewById(R.id.my_items_area_linearlayout);
        mCommonTotalSumView = (TextView)getView().findViewById(R.id.common_total_sum_edittext);
        mMyTotalSumView = (TextView)getView().findViewById(R.id.my_total_sum_edittext);
        mTipPercentView = (EditText)getView().findViewById(R.id.tip_percent_edittext);
        mTipSumView = (EditText)getView().findViewById(R.id.tip_sum_edittext);
        mPassCodeView = (TextView)getView().findViewById(R.id.passcode_textview);

        mCommonItemsCount = (TextView)getView().findViewById(R.id.common_items_count);
        mMyItemsCount = (TextView)getView().findViewById(R.id.my_items_count);

        mScreenSpliter = (ImageView)getView().findViewById(R.id.summary_screen_spliter);

        ScrollView mCommonItemsContainer = (ScrollView) getView().findViewById(R.id.common_summary_area);
        ScrollView mMyItemsContainer = (ScrollView) getView().findViewById(R.id.my_summary_area);

        mUiUpdater = new UiUpdater(mSessionId, mContext, getActivity());
        mPassCodeView.setText("מס' חשבון: " + Integer.toString(mPassCode));

        if(mMainUserMode){
            mUiUpdater.StartMainUser(mDbPath, mCommonItemsArea, mMyItemsArea, mCommonItemsContainer, mMyItemsContainer, mBillRows, mMyTotalSumView, mCommonTotalSumView,
                    mTipPercentView, mTipSumView, mCommonItemsCount, mMyItemsCount, mScreenSpliter, mScreenWidth);
        }else {
            mUiUpdater.StartSecondaryUser(mDbPath, mStoragePath, mCommonItemsArea, mMyItemsArea, mCommonItemsContainer, mMyItemsContainer,  mMyTotalSumView, mCommonTotalSumView,
                    mTipPercentView, mTipSumView, mCommonItemsCount, mMyItemsCount, mScreenSpliter);
        }

        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener( new View.OnKeyListener()
        {
            @Override
            public boolean onKey( View v, int keyCode, KeyEvent event )
            {
//            if(keyCode == KeyEvent.KEYCODE_BACK){
//                mUiUpdater.MoveMyItemsToCommonItems();
//                return true;
//            }
                return false;
            }
        } );
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {

    }


}