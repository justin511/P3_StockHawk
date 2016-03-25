package com.sam_chordas.android.stockhawk.ui;


import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoryColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    private final String LOG_TAG = DetailActivity.class.getSimpleName();
    private static final int CURSOR_LOADER_ID = 1;

    String mSymbol;
    LineChartView mChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.charts);
//        getActionBar().setDisplayHomeAsUpEnabled(true);

        // add the about fragment
        if (savedInstanceState == null) {

            Intent intent = getIntent();
            mSymbol = intent.getExtras().getString("symbol");
            mChart = (LineChartView) findViewById(R.id.chart1);

            getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        }


    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, QuoteProvider.History.CONTENT_URI,
                new String[] {"DISTINCT " + HistoryColumns.SYMBOL, HistoryColumns.DATE,
                        HistoryColumns.CLOSE, HistoryColumns.VOLUME},
                HistoryColumns.SYMBOL + " = ?",
                new String[] {mSymbol},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // todo swap
        if (data != null && data.getCount() != 0) {

//            String[] mLabels = {"April 3", "April 4", "April 5", "April 6"};
//            float[] mValues = {33.0f,33.4f,33.2f,33.4f};

//            ArrayList<String> labelsArrayList = new ArrayList<>();
//            ArrayList<Float> valuesArrayList = new ArrayList<>();

            String[] mLabels = new String[data.getCount()];
            float[] mValues = new float[data.getCount()];

            Log.i(LOG_TAG, "cursor size: " + data.getCount());

            int i = 0;
            while (data.moveToNext()) {
//                labelsArrayList.add(data.getString(data.getColumnIndex(HistoryColumns.DATE)));
//                valuesArrayList.add(Float.parseFloat(data.getString(data.getColumnIndex(HistoryColumns.CLOSE))));
                mLabels[i] =  data.getString(data.getColumnIndex(HistoryColumns.DATE));
                Log.i(LOG_TAG, "mLabels: " + i + " " + mLabels[i]);

                mValues[i++] = Float.parseFloat(data.getString(data.getColumnIndex(HistoryColumns.CLOSE)));
                Log.i(LOG_TAG, "mValues: " + (i-1) + " " + mValues[i-1]);
            }

//
////            mLabels = (String[]) labelsArrayList.toArray();
//            mValues = new float[valuesArrayList.size()];
//
//            int i = 0;
//            for (Float f : valuesArrayList) {
//                mValues[i++] = (f != null ? f : f.NaN);
//            }


            Log.i(LOG_TAG, "onLoadFinished mLabels: " + mLabels.toString());
            Log.i(LOG_TAG, "onLoadFinished mValues: " + mValues.toString());
            // todo check if this is working















//            String[] mLabels = {"Mar 3", "Mar 4", "Mar 5", "Mar 6"};
//            float[] mValues = {33.0f,33.4f,33.2f,33.4f};

            // yellow lines
//            LineSet dataset = new LineSet(mLabels, mValues);
//            dataset.setColor(Color.parseColor("#b3b5bb"))
//                    .setFill(Color.parseColor("#2d374c"))
//                    .setDotsColor(Color.parseColor("#ffc755"))
//                    .setThickness(4)
//                    .endAt(mLabels.length);
//            mChart.addData(dataset);
//
//            // Chart
//            mChart.setBorderSpacing(Tools.fromDpToPx(15))
//                    .setAxisBorderValues(
//                            (int) Utils.getSmallestInArray(mValues) - 10,
//                            (int) Utils.getLargestInArray(mValues) + 10)
//                    .setYLabels(AxisController.LabelPosition.OUTSIDE)
//                    .setLabelsColor(Color.parseColor("#6a84c3"))
//                    .setXAxis(false)
//                    .setYAxis(false);
//
//            mChart.show();
        }


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // todo
    }


}
