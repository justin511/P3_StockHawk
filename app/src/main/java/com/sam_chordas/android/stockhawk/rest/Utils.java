package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoryColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

  private static String LOG_TAG = Utils.class.getSimpleName();

  public static boolean showPercent = true;


  public static ArrayList quoteJsonToContentVals(String JSON, Context c){
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    Log.i(LOG_TAG, "GET FB: " +JSON);
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query");
        int count = Integer.parseInt(jsonObject.getString("count"));
        // if there is only 1 result, data is stored under quote
        if (count == 1){
          jsonObject = jsonObject.getJSONObject("results")
              .getJSONObject("quote");
          // handle case if symbol is not existent
          if (jsonObject.getString("LastTradeDate") != "null") {
            batchOperations.add(buildQuoteBatchOperation(jsonObject, c));
          } else {
            return null;
          }
        // if there is more than 1 result, data is stored under int within quote
          //todo // handle case if symbol is not existent
        } else{
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              Log.i(LOG_TAG, "GET RESULTS: " + jsonObject.toString());
              batchOperations.add(buildQuoteBatchOperation(jsonObject, c));
            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
      //todo a toast to notify user why no stock list populated
    }
    return batchOperations;
  }

  // todo create historyJsonToContentVals method
  public static ArrayList historyJsonToContentVals(String JSON) {
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    Log.i(LOG_TAG, "GET historyJson: " +JSON);
    try {
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0) {
        jsonObject = jsonObject.getJSONObject("query");
        int count = jsonObject.getInt("count");
        if (count == 1) {
          jsonObject = jsonObject.getJSONObject("results").getJSONObject("quote");
          batchOperations.add(buildHistoryBatchOperation(jsonObject));
        } else {
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");
          if (resultsArray != null && resultsArray.length() != 0) {
            for (int i = 0; i < resultsArray.length(); i++) {
              jsonObject = resultsArray.getJSONObject(i);
              batchOperations.add(buildHistoryBatchOperation(jsonObject));
            }
          }
        }
      }
    } catch (JSONException e) {
      Log.e(LOG_TAG, "String to JSON failed: " + e);
    }
    return batchOperations;
  }


  // two digits after decimal point
  public static String truncateBidPrice(String bidPrice){
    bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
    return bidPrice;
  }

  public static String truncateChange(String change, boolean isPercentChange, Context c){
    // need to handle null case: app has crashed due to result with "ChangeinPercent":null
    if (change != "null") {
      // substring: start inclusive, end exclusive
      String sign = change.substring(0, 1);  // + or -
      String percentSign = "";  // % - percent sign
      if (isPercentChange) {
        percentSign = change.substring(change.length() - 1, change.length());
        change = change.substring(0, change.length() - 1);
      }
      change = change.substring(1, change.length());  // getting just the numbers (without sign or %)
      double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;   // 2.98 --> 298.0 --> round --> 2.98
      change = String.format("%.2f", round);
      StringBuffer changeBuffer = new StringBuffer(change); // 2.98
      changeBuffer.insert(0, sign); // +2.98
      changeBuffer.append(percentSign); // +2.98% or +2.98 (if isPercentChange = false)
      change = changeBuffer.toString();
      return change;
    } else {
      return c.getString(R.string.server_error);  //todo use string resource
    }
  }

  public static ContentProviderOperation buildQuoteBatchOperation(JSONObject jsonObject, Context c){
    // content://com.sam_chordas.android.stockhawk.data.QuoteProvider.quotes
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
            QuoteProvider.Quotes.CONTENT_URI);
    try {
      String change = jsonObject.getString("Change");
      builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
      builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
      builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
          jsonObject.getString("ChangeinPercent"), true, c));
      builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false, c));
      builder.withValue(QuoteColumns.ISCURRENT, 1);
      if (change.charAt(0) == '-'){
        builder.withValue(QuoteColumns.ISUP, 0);
      }else{
        builder.withValue(QuoteColumns.ISUP, 1);
      }

    } catch (JSONException e){
      e.printStackTrace();
    }
    return builder.build();
  }


  // todo create historyJsonToContentVals method
  public static ContentProviderOperation buildHistoryBatchOperation(JSONObject jsonObject) {
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
            QuoteProvider.History.CONTENT_URI);
    try {
      builder.withValue(HistoryColumns.SYMBOL, jsonObject.getString("Symbol"));
      builder.withValue(HistoryColumns.DATE, jsonObject.getString("Date"));
      builder.withValue(HistoryColumns.CLOSE, jsonObject.getString("Close"));
      builder.withValue(HistoryColumns.VOLUME, jsonObject.getString("Volume"));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return builder.build();
  }

  /***
   * Used in ChartCard class to set appropriate max & min graph limits
   * @param numbers
   * @return
   */
  public static float getLargestInArray(float[] numbers) {
    float largest = numbers[0];

    for (int i=1; i<numbers.length; i++) {
      if (numbers[i] > largest) {
        largest = numbers[i];
      }
    }

    return largest;
  }

  public static float getSmallestInArray(float[] numbers) {
    float smallest = numbers[0];

    for (int i=1; i<numbers.length; i++) {
      if (numbers[i] < smallest) {
        smallest = numbers[i];
      }
    }

    return smallest;
  }


  /***
   * Format date for chart x-axis
   * @return Date String in format "Mar 09"
   */

  public static String getFormattedDate(String startDateString) {
    DateFormat pdf = new SimpleDateFormat("yyyy-MM-dd");
    DateFormat df = new SimpleDateFormat("MMM d");
    Date startDate;
//    Log.i("Utils", "startDateString: " + startDateString);

    try {
      startDate = pdf.parse(startDateString);
//      Log.i("Utils", "startDate: " + startDate.toString());
      return df.format(startDate);
    } catch (ParseException e){
      e.printStackTrace();
    }
    return  startDateString;
  }

  /***
   * Get date to use to query Yahoo's api
   * @param incrementDate 0 = today; -1 = yesterday; +1 = tomorrow
   * @return date formatted "yyyy-MM-dd" or "2016-03-26"
   */
  public static String getDateRelativeToToday(int incrementDate) {
    Calendar c = Calendar.getInstance();
    long currentTime = System.currentTimeMillis();
    c.setTimeInMillis(currentTime);

    c.add(c.DATE, incrementDate);

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    String date = df.format(c.getTime());

    return date;
  }

  /**
   * Returns true if the network is available or about to become available.
   *
   * @param c Context used to get the ConnectivityManager
   * @return true if the network is available
   */
  public static boolean isNetworkAvailable(Context c) {
    ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return activeNetwork != null &&
            activeNetwork.isConnectedOrConnecting();
  }

  @SuppressWarnings("ResourceType")
  public static @StockTaskService.LocationStatus int getLocationStatus(Context c) {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
    return sp.getInt(c.getString(R.string.pref_location_status_key),
            StockTaskService.LOCATION_STATUS_UNKNOWN);
  }



}
