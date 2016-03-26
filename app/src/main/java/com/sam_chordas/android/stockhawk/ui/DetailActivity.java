package com.sam_chordas.android.stockhawk.ui;


import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoryColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

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

            setTitle(mSymbol);

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

            int cursorCount = data.getCount();

            String[] mLabels = new String[cursorCount];
            float[] mValues = new float[cursorCount];

            Log.i(LOG_TAG, "cursor size: " + cursorCount);

            int i = cursorCount - 1;
            while (data.moveToNext()) {

                //todo format date
                mLabels[i] =  Utils.getFormattedDate(
                        data.getString(data.getColumnIndex(HistoryColumns.DATE)));
                Log.i(LOG_TAG, "mLabels: " + i + " " + mLabels[i]);

                mValues[i--] = Float.parseFloat(
                        data.getString(data.getColumnIndex(HistoryColumns.CLOSE)));
                Log.i(LOG_TAG, "mValues: " + (i+1) + " " + mValues[i+1]);
            }




            //todo gridlines for chart

            // yellow lines
            LineSet dataset = new LineSet(mLabels, mValues);
            dataset.setColor(Color.parseColor("#b3b5bb"))
                    .setFill(Color.parseColor("#2d374c"))
                    .setDotsColor(Color.parseColor("#ffc755"))
                    .setThickness(4)
                    .endAt(mLabels.length);
            mChart.addData(dataset);

            // Chart
            int rangeSmallestToLargest = (int) (Utils.getLargestInArray(mValues)
                    - Utils.getSmallestInArray(mValues));
            mChart.setBorderSpacing(Tools.fromDpToPx(15))
                    .setAxisBorderValues(
                            (int) Utils.getSmallestInArray(mValues),
                            (int) Utils.getLargestInArray(mValues) + rangeSmallestToLargest/2)
                    .setYLabels(AxisController.LabelPosition.OUTSIDE)
                    .setLabelsColor(Color.parseColor("#6a84c3"))
                    .setXAxis(false)
                    .setYAxis(false);

            mChart.show();
        }


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // todo
    }


}
