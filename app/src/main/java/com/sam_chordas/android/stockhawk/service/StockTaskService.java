package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
  private String LOG_TAG = StockTaskService.class.getSimpleName();

  private OkHttpClient client = new OkHttpClient();
  private Context mContext;
  private StringBuilder mStoredSymbols = new StringBuilder();
  private boolean isUpdate;

  private Handler handler;

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({LOCATION_STATUS_OK, LOCATION_STATUS_SERVER_DOWN, LOCATION_STATUS_NON_EXISTENT_STOCK, LOCATION_STATUS_UNKNOWN})
  public @interface LocationStatus {}

  public static final int LOCATION_STATUS_OK = 0;
  public static final int LOCATION_STATUS_SERVER_DOWN = 1;
  public static final int LOCATION_STATUS_NON_EXISTENT_STOCK = 2;
  public static final int LOCATION_STATUS_UNKNOWN = 3;

  public static final String ACTION_DATA_UPDATED =
          "com.sam_chordas.android.stockhawk.app.ACTION_DATA_UPDATED";

  public StockTaskService(){}

  public StockTaskService(Context context){
    mContext = context;
  }
  String fetchData(String url) throws IOException{
    Request request = new Request.Builder()
        .url(url)
        .build();

    Response response = client.newCall(request).execute();
    return response.body().string();
  }

  @Override
  public int onRunTask(TaskParams params){
    Cursor initQueryCursor;
    if (mContext == null){
      mContext = this;
    }
    StringBuilder urlStringBuilder = new StringBuilder();
    try{
      // Base URL for the Yahoo query
      urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");

      if (params.getTag().equals("init") || params.getTag().equals("periodic") ||
              params.getTag().equals("add")) {
        // https://query.yahooapis.com/v1/public/yql?q=select+*+from+yahoo.finance.quotes+where+symbol+in+%28
        urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                + "in (", "UTF-8"));
      } else if (params.getTag().equals("history")) {
        // https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.historicaldata%20where%20symbol%20%3D%20%22YHOO%22%20and%20startDate%20%3D%20%222016-03-02%22%20and%20endDate%20%3D%20%222016-03-09%22&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys
        // select * from yahoo.finance.historicaldata where symbol = "YHOO" and startDate = "2016-03-02" and endDate = "2016-03-09"
        urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where "
                + "symbol = ", "UTF-8"));
      }

    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    if (params.getTag().equals("init") || params.getTag().equals("periodic")){
      isUpdate = true;
      initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
          new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,
          null, null);
      if (initQueryCursor.getCount() == 0 || initQueryCursor == null){
        // Init task. Populates DB with quotes for the symbols seen below
        try {
          urlStringBuilder.append(
              URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      } else if (initQueryCursor != null){
        DatabaseUtils.dumpCursor(initQueryCursor);
        initQueryCursor.moveToFirst();
        for (int i = 0; i < initQueryCursor.getCount(); i++){
          mStoredSymbols.append("\""+
              initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol"))+"\",");
          initQueryCursor.moveToNext();
        }
        mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
        try {
          urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }
    } else if (params.getTag().equals("add")){
      isUpdate = false;
      // get symbol from params.getExtra and build query
      String stockInput = params.getExtras().getString("symbol");
      try {
        // https://query.yahooapis.com/v1/public/yql?q=select+*+from+yahoo.finance.quotes+where+symbol+in+%28
        // UTF-8 is the encoding scheme: " " --> "+"; "(" --> %28; ")" --> %29; """ --> %22
        urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
        // https://query.yahooapis.com/v1/public/yql?q=select+*+from+yahoo.finance.quotes+where+symbol+in+%28%22aapl%22%29
      } catch (UnsupportedEncodingException e){
        e.printStackTrace();
      }
    } else if (params.getTag().equals("history")){
      isUpdate = false;
      // https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.historicaldata%20where%20symbol%20%3D%20%22YHOO%22%20and%20startDate%20%3D%20%222016-03-02%22%20and%20endDate%20%3D%20%222016-03-09%22&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys
      // select * from yahoo.finance.historicaldata where symbol = "YHOO" and startDate = "2016-03-02" and endDate = "2016-03-09"
      String stockInput = params.getExtras().getString("symbol");
      try {
        urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\"", "UTF-8"));
        urlStringBuilder.append(URLEncoder.encode(
                " and startDate = \"" + Utils.getDateRelativeToToday(-10) + "\"", "UTF-8"));
        urlStringBuilder.append(URLEncoder.encode(
                " and endDate = \"" + Utils.getDateRelativeToToday(0) + "\"", "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
    }

    // finalize the URL for the API query.
    // &format=json&&diagnostics=true&env=store://datatables.org/alltableswithkeys
    urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
        + "org%2Falltableswithkeys&callback=");

    String urlString;
    String getResponse;
    int result = GcmNetworkManager.RESULT_FAILURE;

    if (urlStringBuilder != null){
      urlString = urlStringBuilder.toString();
//      Log.i(LOG_TAG, "URL String: " + urlString); // debug purpose

      try{
        getResponse = fetchData(urlString);
        result = GcmNetworkManager.RESULT_SUCCESS;

        try {
          // if tag is "init" or "periodic", isUpdate = true
          if (isUpdate){
            ContentValues contentValues = new ContentValues();
            contentValues.put(QuoteColumns.ISCURRENT, 0);   // update ISCURRENT to 0 (false) so new data is current
            mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                null, null);
          }
          // update database
          if (params.getTag().equals("init") || params.getTag().equals("periodic") || params.getTag().equals("add")) {
            if (Utils.quoteJsonToContentVals(getResponse, mContext) != null) {
              mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                      Utils.quoteJsonToContentVals(getResponse, mContext));

              Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
                      .setPackage(mContext.getPackageName());
              mContext.sendBroadcast(dataUpdatedIntent);

            } else {
              Handler handler = new Handler(mContext.getMainLooper());
              handler.post(new Runnable() {
                @Override
                public void run() {
                  Toast toast = Toast.makeText(mContext, mContext.getString(R.string.toast_invalid_stock),
                              Toast.LENGTH_LONG);
                  toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                  toast.show();
                }
              });
              setLocationStatus(mContext, LOCATION_STATUS_NON_EXISTENT_STOCK); // non-existent stock
            }
          } else {
            mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                    Utils.historyJsonToContentVals(getResponse));
          }
          setLocationStatus(mContext, LOCATION_STATUS_OK);
        }catch (RemoteException | OperationApplicationException e){
          Log.e(LOG_TAG, "Error applying batch insert", e);
        }

      } catch (IOException e){
        e.printStackTrace();
        setLocationStatus(mContext, LOCATION_STATUS_SERVER_DOWN);
      }
    }

    return result;  // GcmNetworkManager success = 0, failure = 2
  }


  /**
   * Sets the location status into shared preference.  This function should not be called from
   * the UI thread because it uses commit to write to the shared preferences.
   * @param c Context to get the PreferenceManager from.
   * @param locationStatus The IntDef value to set
   */
  private static void setLocationStatus(Context c, @LocationStatus int locationStatus) {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
    SharedPreferences.Editor spe = sp.edit();
    spe.putInt(c.getString(R.string.pref_location_status_key), locationStatus);
    spe.commit();
  }

}
