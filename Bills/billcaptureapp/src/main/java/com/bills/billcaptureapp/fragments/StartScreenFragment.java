package com.bills.billcaptureapp.fragments;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bills.billcaptureapp.R;
import com.bills.billslib.Contracts.Constants;
import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Enums.LogsDestination;
import com.bills.billslib.Core.BillsLog;
import com.bills.billslib.Utilities.Utilities;

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
    String mRestaurantName;
    private OnFragmentInteractionListener mListener;
    public enum CaptureType{
        SIMPLE,
        RIGHT,
        LEFT,
        REMOTLY,
        STRAIGHT
    }

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
    public void onDestroyView() {
        super.onDestroyView();
        BillsLog.Log(Tag, LogLevel.Info, "onDestroyView", LogsDestination.BothUsers);
    }

    @Override
    public void onClick(View v) {}

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState)     {
        //Initialize parameters
        _clickToSimpleCapture = view.findViewById(R.id.simpleCaptureButton);
        _clickToRightCapture = view.findViewById(R.id.rightCaptureButton);
        _clickToLeftCapture = view.findViewById(R.id.leftCaptureButton);
        _clickToRemotlyCapture = view.findViewById(R.id.remotlyCaptureButton);
        _clickToStraightCapture = view.findViewById(R.id.straightCaptureButton);
        SetOnClickListenerSimpleCaptureButton();
        SetOnClickListenerRightCaptureButton();
        SetOnClickListenerLeftCaptureButton();
        SetOnClickListenerRemotlyCaptureButton();
        SetOnClickListenerStraightCaptureButton();
        SetOnEditorActionListener(view);
    }

    private void SetOnEditorActionListener(final View view) {
        ((EditText)view.findViewById(R.id.restaurantName)).setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                actionId == EditorInfo.IME_ACTION_DONE ||
                                (null != event &&
                                 event.getAction() == KeyEvent.ACTION_DOWN &&
                                 event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                            mRestaurantName = v.getText().toString();
                            String folderToCreate = Constants.TESSERACT_SAMPLE_DIRECTORY + Build.BRAND + "_" + Build.MODEL + "/" + mRestaurantName;
                            if (null != event && !event.isShiftPressed()) {
                                boolean isFolderCreated = Utilities.CreateDirectory(folderToCreate);
                                HideSoftKeyboard(view);
                                String toToast = isFolderCreated ?
                                        "Folder "+ folderToCreate + " created" :
                                        "Folder "+ folderToCreate + " already exists!";
                                Toast.makeText(view.getContext(), toToast, Toast.LENGTH_LONG).show();
                                return true; // consume.
                            }else if(null == event &&
                                     actionId == EditorInfo.IME_ACTION_DONE){
                                boolean isFolderCreated = Utilities.CreateDirectory(folderToCreate);
                                HideSoftKeyboard(view);
                                String toToast = isFolderCreated ?
                                         "Folder "+ folderToCreate + " created" :
                                         "Folder "+ folderToCreate + " already exists!";
                                Toast.makeText(view.getContext(), toToast, Toast.LENGTH_LONG).show();
                                return true;
                            }
                        }
                        return false; // pass on to other listeners.
                    }
                });
    }

    void HideSoftKeyboard(View view){
        InputMethodManager inputManager =
                (InputMethodManager) view.getContext().
                        getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(
                view.getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public void SetOnClickListenerSimpleCaptureButton() {
        _clickToSimpleCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    mListener.StartCameraFragment();
                    mListener.NotifyClickedButton(CaptureType.SIMPLE, mRestaurantName);
                } catch (Exception e) {
                    BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage(), LogsDestination.BothUsers);
                }
            }
        });
    }

    public void SetOnClickListenerRightCaptureButton() {
        _clickToRightCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    mListener.StartCameraFragment();
                    mListener.NotifyClickedButton(CaptureType.RIGHT, mRestaurantName);
                } catch (Exception e) {
                    BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage(), LogsDestination.BothUsers);
                }
            }
        });
    }

    public void SetOnClickListenerLeftCaptureButton() {
        _clickToLeftCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    mListener.StartCameraFragment();
                    mListener.NotifyClickedButton(CaptureType.LEFT, mRestaurantName);
                } catch (Exception e) {
                    BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage(), LogsDestination.BothUsers);
                }
            }
        });
    }

    public void SetOnClickListenerRemotlyCaptureButton() {
        _clickToRemotlyCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    mListener.StartCameraFragment();
                    mListener.NotifyClickedButton(CaptureType.REMOTLY, mRestaurantName);
                } catch (Exception e) {
                    BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage(), LogsDestination.BothUsers);
                }
            }
        });
    }

    public void SetOnClickListenerStraightCaptureButton() {
        _clickToStraightCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    mListener.StartCameraFragment();
                    mListener.NotifyClickedButton(CaptureType.STRAIGHT, mRestaurantName);
                } catch (Exception e) {
                    BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage(), LogsDestination.BothUsers);
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
        void NotifyClickedButton(CaptureType captureType, String mRestaurantName);
    }
}
