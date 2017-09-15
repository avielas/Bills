package com.bills.bills.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bills.bills.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link WelcomeScreenFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class WelcomeScreenFragment extends Fragment implements View.OnClickListener{

    private OnFragmentInteractionListener mListener;

    public WelcomeScreenFragment() {
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
        return inflater.inflate(R.layout.fragment_welcome_screen, container, false);
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
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)     {
        ((Button)view.findViewById(R.id.start_camera_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.StartCameraFragment();
            }
        });
        ((Button)view.findViewById(R.id.check_pass_code_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int passCode = Integer.parseInt(((EditText) getView().findViewById(R.id.pass_code_edittext)).getText().toString());
                    mListener.StartSummarizerFragment(passCode);
                } catch (Exception ex) {
                    Toast.makeText(getActivity(), "Invalid pass code.Try again", Toast.LENGTH_SHORT).show();
                    return;
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

        void StartSummarizerFragment(int passCode);

    }
}
