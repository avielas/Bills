package com.bills.bills.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
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

        ImageView city1 = (ImageView) getActivity().findViewById(R.id.city_bg_image_view);

        Animation animation
                = AnimationUtils.loadAnimation(getContext(), R.anim.move);
        city1.startAnimation(animation);

        ImageView city2 = (ImageView)getActivity().findViewById(R.id.city_bg_l2_image_view);

        Animation animation2 = AnimationUtils.loadAnimation(getContext(), R.anim.move2);
        city2.startAnimation(animation2);

        view.findViewById(R.id.start_camera_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mListener.StartCamera();
            }
        });
        view.findViewById(R.id.check_pass_code_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int passCode = Integer.parseInt(((EditText) getView().findViewById(R.id.pass_code_edittext)).getText().toString());
                    mListener.StartSummarizer(passCode);
                } catch (Exception ex) {
                    Toast.makeText(getActivity(), "Invalid pass code.Try again", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

        final EditText passCodeTextView = (EditText)getView().findViewById(R.id.pass_code_edittext);
        passCodeTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    InputMethodManager input = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    input.hideSoftInputFromWindow(passCodeTextView.getWindowToken(), 0);
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

        void StartCamera();

        void StartSummarizer(int passCode);


    }
}
