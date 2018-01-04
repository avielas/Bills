package com.bills.bills.fragments;

import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.bills.bills.R;
import com.bills.billslib.CustomViews.DragRectView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DragRectViewFragment.OnDragRectViewInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DragRectViewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DragRectViewFragment extends Fragment {
    private static final String TOP_LEFT = "topLeft";
    private static final String TOP_RIGHT = "topRight";
    private static final String BOTTOM_RIGHT = "buttomRight";
    private static final String BOTTOM_LEFT = "buttomLeft";

    private Point mTopLeft;
    private Point mTopRight;
    private Point mBottomRight;
    private Point mBottomLeft;

    private OnDragRectViewInteractionListener mListener;

    private DragRectView mDragRectView;

    public DragRectViewFragment() {
        // Required empty public constructor
    }


    public static DragRectViewFragment newInstance(Point topLeft, Point topRight, Point bottomRight, Point bottomLeft) {
        DragRectViewFragment fragment = new DragRectViewFragment();
        Bundle args = new Bundle();
        args.putParcelable(TOP_LEFT, topLeft);
        args.putParcelable(TOP_RIGHT, topRight);
        args.putParcelable(BOTTOM_RIGHT, bottomRight);
        args.putParcelable(BOTTOM_LEFT, bottomLeft);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDragRectView = getActivity().findViewById(R.id.dragRectView);

        Button doneButton = getActivity().findViewById(R.id.dragRectFragmentDone);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onDragRectViewFinished(mDragRectView.TopLeft, mDragRectView.TopRight,
                            mDragRectView.ButtomRight, mDragRectView.ButtomLeft);
                }
            }
        });

        if (getArguments() != null) {
            mTopLeft = getArguments().getParcelable(TOP_LEFT);
            mTopRight = getArguments().getParcelable(TOP_RIGHT);
            mBottomRight = getArguments().getParcelable(BOTTOM_RIGHT);
            mBottomLeft = getArguments().getParcelable(BOTTOM_LEFT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_drag_rect_view, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDragRectViewInteractionListener) {
            mListener = (OnDragRectViewInteractionListener) context;
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
    public interface OnDragRectViewInteractionListener {
        void onDragRectViewFinished(Point topLeft, Point topRight, Point bottomRight, Point bottomLeft);
    }
}
