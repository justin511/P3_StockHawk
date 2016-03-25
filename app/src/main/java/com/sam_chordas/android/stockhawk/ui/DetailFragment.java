package com.sam_chordas.android.stockhawk.ui;



import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoryColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.graph.ChartCard;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private final String LOG_TAG = DetailFragment.class.getSimpleName();

    private String mSymbol;

    private ChartCard mChartCard;

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
//        (new ChartCard((CardView) layout.findViewById(R.id.card1), getContext(), mSymbol)).init();
        ChartCard mChartCard = new ChartCard((CardView) layout.findViewById(R.id.card1), getContext(), mSymbol);
        mChartCard.init();

//        getLoaderManager().initLoader(ChartCard.CURSOR_LOADER_ID, null, this);


        return layout;


    }




    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext(), QuoteProvider.History.CONTENT_URI,
                new String[] {HistoryColumns._ID, HistoryColumns.SYMBOL, HistoryColumns.DATE,
                        HistoryColumns.CLOSE, HistoryColumns.VOLUME},
                HistoryColumns.SYMBOL + " = ?",
                new String[] {mSymbol},  // todo use mSymbol
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // todo swap
        if (data != null && data.getCount() != 0) {

            ArrayList<String> labelsArrayList = new ArrayList<>();
            ArrayList<Float> valuesArrayList = new ArrayList<>();

            while (data.moveToNext()) {
                labelsArrayList.add(data.getString(data.getColumnIndex(HistoryColumns.DATE)));
                valuesArrayList.add(Float.parseFloat(data.getString(data.getColumnIndex(HistoryColumns.CLOSE))));
            }

            String[] mLabels = {"April 3", "April 4", "April 5", "April 6"};
            float[] mValues = {33.0f,33.4f,33.2f,33.4f};

//            mLabels = (String[]) labelsArrayList.toArray();
            mValues = new float[valuesArrayList.size()];

            int i = 0;
            for (Float f : valuesArrayList) {
                mValues[i++] = (f != null ? f : f.NaN);
            }

            mChartCard.setmLabels(mLabels);
            mChartCard.setmValues(mValues);

            Log.i(LOG_TAG, "onLoadFinished mLabels: " + mLabels.toString());
            Log.i(LOG_TAG, "onLoadFinished mValues: " + mValues.toString());
            // todo check if this is working

            mChartCard.update();
        }


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // todo
    }

}
