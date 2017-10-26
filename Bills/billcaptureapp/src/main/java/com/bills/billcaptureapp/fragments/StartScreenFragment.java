package com.bills.billcaptureapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.bills.billcaptureapp.R;
import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Core.BillsLog;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StartScreenFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class StartScreenFragment extends Fragment implements View.OnClickListener{
    private String Tag = StartScreenFragment.class.getName();
    Button _clickToSimpleCapture;
    Button _clickToRightCapture;
    Button _clickToLeftCapture;
    Button _clickToRemotlyCapture;
    Button _clickToStraightCapture;
    private OnFragmentInteractionListener mListener;

    public StartScreenFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start_screen, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        _clickToSimpleCapture = null;
        _clickToRightCapture = null;
        _clickToLeftCapture = null;
        _clickToRemotlyCapture = null;
        _clickToStraightCapture = null;
        mListener = null;
    }

    @Override
    public void onClick(View v) {}

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)     {
        //Initialize parameters
        _clickToSimpleCapture = view.findViewById(R.id.simpleCaptureButton);
        _clickToRightCapture = view.findViewById(R.id.rightCaptureButton);
        _clickToLeftCapture = view.findViewById(R.id.leftCaptureButton);
        _clickToRemotlyCapture = view.findViewById(R.id.remotlyCaptureButton);
        _clickToStraightCapture = view.findViewById(R.id.straightCaptureButton);
        AddListenerToSimpleCaptureButton();
        AddListenerToRightCaptureButton();
        AddListenerToLeftCaptureButton();
        AddListenerToRemotlyCaptureButton();
        AddListenerToStraightCaptureButton();
    }

    public void AddListenerToSimpleCaptureButton() {
        _clickToSimpleCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    mListener.StartCameraFragment();
                } catch (Exception e) {
                    BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage());
                }
            }
        });
    }

    public void AddListenerToRightCaptureButton() {
        _clickToRightCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    mListener.StartCameraFragment();
                } catch (Exception e) {
                    BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage());
                }
            }
        });
    }

    public void AddListenerToLeftCaptureButton() {
        _clickToLeftCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    mListener.StartCameraFragment();
                } catch (Exception e) {
                    BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage());
                }
            }
        });
    }

    public void AddListenerToRemotlyCaptureButton() {
        _clickToRemotlyCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    mListener.StartCameraFragment();
                } catch (Exception e) {
                    BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage());
                }
            }
        });
    }

    public void AddListenerToStraightCaptureButton() {
        _clickToStraightCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    mListener.StartCameraFragment();
                } catch (Exception e) {
                    BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage());
                }
            }
        });
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
        void StartCameraFragment();
    }
}
