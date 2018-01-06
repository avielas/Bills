package com.bills.testslib;
import android.support.v4.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnCameraFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class CameraFragment extends com.bills.billslib.Fragments.CameraFragment {
    @Override
    public void OnCameraFinished(byte[] image) {

        //DANGEROUS CAST!!!!!!
        ((AvielOnFragmentInteractionListener)mListener).ReturnToWelcomeScreen(image);
    }

    public interface AvielOnFragmentInteractionListener extends OnCameraFragmentInteractionListener {
        void ReturnToWelcomeScreen(final byte[] image);
    }
}
