package com.sam_chordas.android.stockhawk.ui;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.ui.graph.ChartCard;

/**
 * A simple {@link Fragment} subclass.
 */
public class DetailFragment extends Fragment {

    private String mSymbol;

    public DetailFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mSymbol = arguments.getString("symbol");
            Log.i("DetailFragment", "mSymbol: " + mSymbol);
        }

        View layout = inflater.inflate(R.layout.charts, container, false);

//        Toolbar toolbar = (Toolbar) layout.findViewById(R.id.toolbar);
//        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        // todo pass symbol through constructor
        (new ChartCard((CardView) layout.findViewById(R.id.card1), getContext(), mSymbol)).init();


        return layout;


    }


}
