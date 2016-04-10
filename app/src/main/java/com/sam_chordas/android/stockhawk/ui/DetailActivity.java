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
    private static final int NUM_OF_DAYS = 5;

    String mSymbol;
    LineChartView mChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.charts);
//        getActionBar().setDisplayHomeAsUpEnabled(true);

//        if (savedInstanceState == null) {

            Intent intent = getIntent();
            mSymbol = intent.getExtras().getString("symbol");
            mChart = (LineChartView) findViewById(R.id.chart1);

            setTitle(mSymbol);

            getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

//        }


    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, QuoteProvider.History.CONTENT_URI,
                new String[] {"DISTINCT " + HistoryColumns.SYMBOL, HistoryColumns.DATE,
                        HistoryColumns.CLOSE, HistoryColumns.VOLUME},
                HistoryColumns.SYMBOL + " = ?",
                new String[] {mSymbol},
                HistoryColumns.DATE + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.getCount() != 0) {

            int cursorCount = data.getCount();

            Log.i(LOG_TAG, "cursor size: " + cursorCount);

            String[] chartLabels = new String[NUM_OF_DAYS];
            float[] chartValues = new float[NUM_OF_DAYS];

//            data.moveToFirst();
            for (int i = NUM_OF_DAYS - 1; i >= 0; i--) {
                data.moveToNext();

                chartLabels[i] =  Utils.getFormattedDate(
                        data.getString(data.getColumnIndex(HistoryColumns.DATE)));
                Log.i(LOG_TAG, "chartLabels: " + i + " " + chartLabels[i]);

                chartValues[i] = Float.parseFloat(
                        data.getString(data.getColumnIndex(HistoryColumns.CLOSE)));
                Log.i(LOG_TAG, "chartValues: " + (i) + " " + chartValues[i]);
            }



            mChart.dismiss();

            //todo gridlines for chart

            // yellow lines
            LineSet dataset = new LineSet(chartLabels, chartValues);
            dataset.setColor(Color.parseColor("#b3b5bb"))
                    .setFill(Color.parseColor("#2d374c"))
                    .setDotsColor(Color.parseColor("#ffc755"))
                    .setThickness(4)
                    .endAt(chartLabels.length);
            mChart.addData(dataset);

            // Chart
            int rangeSmallestToLargest = Math.round((Utils.getLargestInArray(chartValues)
                    - Utils.getSmallestInArray(chartValues)));

            mChart.setBorderSpacing(Tools.fromDpToPx(15))
                    .setAxisBorderValues(
                            (int) Utils.getSmallestInArray(chartValues),
                            (int) Utils.getLargestInArray(chartValues)
                                    + Math.max(rangeSmallestToLargest / 2, 1))
                    .setYLabels(AxisController.LabelPosition.OUTSIDE)
                    .setLabelsColor(Color.parseColor("#6a84c3"))
                    .setXAxis(false)
                    .setYAxis(false);

            mChart.show();

        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }


}
