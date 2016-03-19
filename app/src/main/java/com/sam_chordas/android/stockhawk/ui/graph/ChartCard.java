package com.sam_chordas.android.stockhawk.ui.graph;

import android.animation.PropertyValuesHolder;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.LineChartView;
import com.db.chart.view.Tooltip;
import com.db.chart.view.animation.Animation;
import com.db.chart.view.animation.easing.BounceEase;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoryColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

import java.util.ArrayList;

/**
 * Created by justinmae on 3/7/16.
 */




public class ChartCard extends CardController implements LoaderManager.LoaderCallbacks<Cursor> {

    private final String LOG_TAG = ChartCard.class.getSimpleName();

    private static final int HISTORY_LOADER_ID = 0;

    private final LineChartView mChart;

    private final Context mContext;

    private String mSymbol;


//    private final String[] mLabels= {"Jan", "Fev", "Mar", "Apr", "Jun", "May", "Jul", "Aug", "Sep"};
//    private final float[][] mValues = {{3.5f, 4.7f, 4.3f, 8f, 6.5f, 9.9f, 7f, 8.3f, 7.0f},
//            {4.5f, 2.5f, 2.5f, 9f, 4.5f, 9.5f, 5f, 8.3f, 1.8f}};

    private String[] mLabels = {"Mar 3", "Mar 4", "Mar 5", "Mar 6"};
    private float[] mValues = {33.0f,33.4f,33.2f,33.4f};


    private Tooltip mTip;

    private Runnable mBaseAction;


    public ChartCard(CardView card, Context context, String symbol){
        super(card);

        mContext = context;
        mChart = (LineChartView) card.findViewById(R.id.chart1);
        mSymbol = symbol;
    }

//    getLoaderManager

// todo get cursor data

    // just need to query for date and price
//    Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
//            new String[] { QuoteColumns.SYMBOL }, QuoteColumns.SYMBOL + "= ?",
//            new String[] { input.toString().toUpperCase() }, null);   // need to normalize and use uppercase of input

    /*
    QuoteProvider.History.CONTENT_URI,
                new String[] {HistoryColumns._ID, HistoryColumns.SYMBOL, HistoryColumns.DATE,
                    HistoryColumns.CLOSE, HistoryColumns.VOLUME},
                HistoryColumns.SYMBOL + " = ?",
                new String[] {"YHOO"},  // todo use mSymbol
                null);
     */


    @Override
    public void show(Runnable action) {
        super.show(action);

        // Tooltip
        mTip = new Tooltip(mContext, R.layout.linechart_three_tooltip, R.id.value);

        ((TextView) mTip.findViewById(R.id.value))
                .setTypeface(Typeface.createFromAsset(mContext.getAssets(), "fonts/Roboto-Medium.ttf"));

        mTip.setVerticalAlignment(Tooltip.Alignment.BOTTOM_TOP);
        mTip.setDimensions((int) Tools.fromDpToPx(65), (int) Tools.fromDpToPx(25));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {

            mTip.setEnterAnimation(PropertyValuesHolder.ofFloat(View.ALPHA, 1),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f),
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1f)).setDuration(200);

            mTip.setExitAnimation(PropertyValuesHolder.ofFloat(View.ALPHA, 0),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f),
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 0f)).setDuration(200);

            mTip.setPivotX(Tools.fromDpToPx(65) / 2);
            mTip.setPivotY(Tools.fromDpToPx(25));
        }

        mChart.setTooltips(mTip);

        // Data
        // grayed out and dotted lines
//        LineSet dataset = new LineSet(mLabels, mValues[0]);
//        dataset.setColor(Color.parseColor("#758cbb"))
//                .setFill(Color.parseColor("#2d374c"))
//                .setDotsColor(Color.parseColor("#758cbb"))
//                .setThickness(4)
//                .setDashed(new float[]{10f,10f})
//                .beginAt(5);
//        mChart.addData(dataset);

        // yellow lines
        LineSet dataset = new LineSet(mLabels, mValues);
        dataset.setColor(Color.parseColor("#b3b5bb"))
                .setFill(Color.parseColor("#2d374c"))
                .setDotsColor(Color.parseColor("#ffc755"))
                .setThickness(4)
                .endAt(mLabels.length);
        mChart.addData(dataset);

        // Chart
        mChart.setBorderSpacing(Tools.fromDpToPx(15))
                .setAxisBorderValues(
                        (int) Utils.getSmallestInArray(mValues) - 10,
                        (int) Utils.getLargestInArray(mValues) + 10)
                .setYLabels(AxisController.LabelPosition.OUTSIDE)
                .setLabelsColor(Color.parseColor("#6a84c3"))
                .setXAxis(false)
                .setYAxis(false);

        // this is the tooltip with price
        mBaseAction = action;
        Runnable chartAction = new Runnable() {
            @Override
            public void run() {
                mBaseAction.run();
                mTip.prepare(mChart.getEntriesArea(0).get(2), mValues[2]);
                mChart.showTooltip(mTip, true);
            }
        };

        Animation anim = new Animation()
                .setEasing(new BounceEase())
                .setEndAction(chartAction);

        mChart.show(anim);
    }


    @Override
    public void update() {
        super.update();

        mChart.dismissAllTooltips();
//        if (firstStage) {
//            mChart.updateValues(0, mValues[1]);
//            mChart.updateValues(1, mValues[1]);
//        }else{
//            mChart.updateValues(0, mValues[0]);
//            mChart.updateValues(1, mValues[0]);
//        }

        mChart.updateValues(0, mValues);
        mChart.updateValues(1, mValues);
        mChart.getChartAnimation().setEndAction(mBaseAction);
        mChart.notifyDataUpdate();
    }


    @Override
    public void dismiss(Runnable action) {
        super.dismiss(action);

        mChart.dismissAllTooltips();
        mChart.dismiss(new Animation()
                .setEasing(new BounceEase())
                .setEndAction(action));
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(mContext, QuoteProvider.History.CONTENT_URI,
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

            mLabels = (String[]) labelsArrayList.toArray();
            mValues = null;

            int i = 0;
            for (Float f : valuesArrayList) {
                mValues[i++] = (f != null ? f : f.NaN);
            }

            Log.i(LOG_TAG, "onLoadFinished mLabels: " + mLabels.toString());
            Log.i(LOG_TAG, "onLoadFinished mValues: " + mValues.toString());
            // todo check if this is working

            mChart.notifyDataUpdate();
        }


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // todo
    }
}
