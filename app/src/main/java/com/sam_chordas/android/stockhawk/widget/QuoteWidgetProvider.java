package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sam_chordas.android.stockhawk.service.StockTaskService;

/**
 * Created by justinmae on 4/7/16.
 */
public class QuoteWidgetProvider extends AppWidgetProvider {

    private String LOG_TAG = QuoteWidgetProvider.class.getSimpleName();

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        context.startService(new Intent(context, QuoteWidgetRemoteViewsService.class));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.i(LOG_TAG, "onReceive called");
        if(StockTaskService.ACTION_DATA_UPDATED.equals(intent.getAction())) {
            Log.i(LOG_TAG, "onReceive starts service");
            context.startService(new Intent(context, QuoteWidgetRemoteViewsService.class));
        }
    }
}
