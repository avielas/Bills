package com.bills.bills.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bills.bills.R;
import com.bills.bills.firebase.UiUpdater;
import com.bills.billslib.Contracts.BillRow;
import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Enums.LogsDestination;
import com.bills.billslib.Core.BillsLog;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BillSummarizerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class BillSummarizerFragment extends Fragment {
    private static String Tag = BillSummarizerFragment.class.getName();
    private int mPassCode;
    private String mDbPath;
    private String mStoragePath;
    private OnFragmentInteractionListener mListener;

    private boolean mMainUserMode;
    private Context mContext;

    private UiUpdater mUiUpdater;

    private LinearLayout mCommonItemsArea;
    private LinearLayout mMyItemsArea;
    private TextView mTotalSumView;
    private EditText mTipView;
    private TextView mPassCodeView;

    private List<BillRow> mBillRows;
    public BillSummarizerFragment() {
        // Required empty public constructor
    }


    //Secondary user
    public void Init(Context context, Integer passCode, String dbPath, String storagePath) {
        mPassCode = passCode;
        mDbPath = dbPath;
        mStoragePath = storagePath;
        mContext = context;
        mMainUserMode = false;
    }

    public void Init(Context context, Integer passCode, String dbPath, List<BillRow> rows) {
        mPassCode = passCode;
        mDbPath = dbPath;
        mContext = context;
        mBillRows = rows;
        mMainUserMode = true;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mCommonItemsArea = (LinearLayout)getView().findViewById(R.id.common_items_area_linearlayout);
        mMyItemsArea = (LinearLayout)getView().findViewById(R.id.my_items_area_linearlayout);
        mTotalSumView = (TextView)getView().findViewById(R.id.my_total_sum_edittext);
        mTipView = (EditText)getView().findViewById(R.id.tip_edittext);
        // TODO: to remove setEnabled(false)!
        // TODO: I added it just for debugging usages
        mTipView.setEnabled(false);
        mPassCodeView = (TextView)getView().findViewById(R.id.passcode_textview);

        if(mMainUserMode){
            mUiUpdater = new UiUpdater();
            mUiUpdater.StartMainUser(mContext, mDbPath, mCommonItemsArea, mMyItemsArea, mBillRows, mTotalSumView, mTipView);
            mPassCodeView.setText(Integer.toString(mPassCode)
            );
        }else {

            mUiUpdater = new UiUpdater();
            mUiUpdater.StartSecondaryUser(mContext, mDbPath, mStoragePath, mCommonItemsArea, mMyItemsArea, mTotalSumView, mTipView);
            mPassCodeView.setText(Integer.toString(mPassCode));
        }
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
            BillsLog.Log(Tag, LogLevel.Error, context.toString() + " must implement OnFragmentInteractionListener", LogsDestination.BothUsers);
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mBillRows = null;
        mListener = null;
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
        void onFragmentInteraction();
    }
}
