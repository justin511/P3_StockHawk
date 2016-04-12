package com.sam_chordas.android.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

/**
 * Created by justinmae on 4/7/16.
 */
public class QuoteWidgetRemoteViewsService extends RemoteViewsService {
    private final String LOG_TAG = QuoteWidgetRemoteViewsService.class.getSimpleName();

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {

            private Cursor data = null;

            @Override
            public void onCreate() {
//                Log.i(LOG_TAG, "RemoteViewService: onCreate");
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
//                Log.i(LOG_TAG, "RemoteViewService: onDataSetChanged");
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                                QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                        QuoteColumns.ISCURRENT + " = ?",
                        new String[]{"1"},
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
//                Log.i(LOG_TAG, "RemoteViewService: onDestroy");
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
//                Log.i(LOG_TAG, "RemoteViewService: getViewAt");
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_collection_item);
                String symbol = data.getString(data.getColumnIndex("symbol"));
                String change = data.getString(data.getColumnIndex("percent_change"));

//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
//                    setRemoteContentDescription(views, description);
//                }


                if (data.getInt(data.getColumnIndex("is_up")) == 1){
                        views.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
                } else{
                        views.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
                }

                views.setTextViewText(R.id.stock_symbol, symbol);
                views.setTextViewText(R.id.change, change);
                views.setContentDescription(R.id.change,
                        getString(R.string.a11y_percent_change, change));

                Bundle extras = new Bundle();
                extras.putString("symbol", symbol);

                Intent fillInIntent = new Intent();
                fillInIntent.putExtras(extras);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_collection_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position)) {
                    return data.getLong(data.getColumnIndex("_id"));
                }
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return false;
            }
        };
    }


}
