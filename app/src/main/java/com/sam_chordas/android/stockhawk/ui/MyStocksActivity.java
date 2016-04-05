package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */

  /**
   * Used to store the last screen title. For use in {@link #restoreActionBar()}.
   */
  private final String LOG_TAG = MyStocksActivity.class.getSimpleName();
  private CharSequence mTitle;
  private Intent mServiceIntent;
  private ItemTouchHelper mItemTouchHelper;
  private static final int CURSOR_LOADER_ID = 0;
  private QuoteCursorAdapter mCursorAdapter;
  private Context mContext;
  private Cursor mCursor;
  boolean isConnected;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = this;
    isConnected = Utils.isNetworkAvailable(this);

    setContentView(R.layout.activity_my_stocks);
    // The intent service is for executing immediate pulls from the Yahoo API
    // GCMTaskService can only schedule tasks, they cannot execute immediately
    mServiceIntent = new Intent(this, StockIntentService.class);
    if (savedInstanceState == null) {
      // Run the initialize task service so that some stocks appear upon an empty database
      mServiceIntent.putExtra("tag", "init");
      if (isConnected) {
        startService(mServiceIntent);
      } else {
        networkToast();
      }
    }
    RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    View emptyView = findViewById(R.id.recycler_view_empty);

    getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

    mCursorAdapter = new QuoteCursorAdapter(this, null, emptyView);  // cursor is null now, but gets swapped later

    recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
            new RecyclerViewItemClickListener.OnItemClickListener() {
              @Override
              public void onItemClick(View v, int position) {
                // get historical price data
                mCursor.moveToPosition(position);   // move to correct row in database
                String symbol = mCursor.getString(mCursor.getColumnIndex("symbol"));
//                Log.e(LOG_TAG, "cursor touch after: " + a);

                Log.e(LOG_TAG, "cursor: " + mCursor.toString());

                mServiceIntent.putExtra("tag", "history");
                mServiceIntent.putExtra("symbol", symbol);
                startService(mServiceIntent);

                // do something on item click
                Intent intent = new Intent(getBaseContext(), DetailActivity.class);
                intent.putExtra("symbol", symbol);
                startActivity(intent);

              }
            }));
    recyclerView.setAdapter(mCursorAdapter);


    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.attachToRecyclerView(recyclerView);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (isConnected) {
          new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                  .content(R.string.content_test)
                  .inputType(InputType.TYPE_CLASS_TEXT)
                  .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                      Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                              new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                              new String[]{input.toString().toUpperCase()}, null);   // need to normalize and use uppercase of input
                      if (c.getCount() != 0) {
                        Toast toast =
                                Toast.makeText(MyStocksActivity.this, "This stock is already saved!",
                                        Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                        toast.show();
                        return;
                      } else if (input.toString().equals("")) {
                        return;
                      } else {
                        // Add the stock to DB
                        mServiceIntent.putExtra("tag", "add");
                        mServiceIntent.putExtra("symbol", input.toString().toUpperCase());    // need to normalize and use uppercase of input
                        startService(mServiceIntent);
                      }
                    }
                  })
                  .show();
        } else {
          networkToast();
        }

      }
    });

    ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
    mItemTouchHelper = new ItemTouchHelper(callback);
    mItemTouchHelper.attachToRecyclerView(recyclerView);

    mTitle = getTitle();
    if (isConnected) {
      long period = 3600L;
      long flex = 10L;
      String periodicTag = "periodic";

      // create a periodic task to pull stocks once every hour after the app has been opened. This
      // is so Widget data stays up to date.
      PeriodicTask periodicTask = new PeriodicTask.Builder()
              .setService(StockTaskService.class)
              .setPeriod(period)
              .setFlex(flex)
              .setTag(periodicTag)
              .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
              .setRequiresCharging(false)
              .build();
      // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
      // are updated.
      GcmNetworkManager.getInstance(this).schedule(periodicTask);
    }
  }


  @Override
  public void onResume() {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
    sp.registerOnSharedPreferenceChangeListener(this);  // Registers a callback to be invoked when a change happens to a preference.
    super.onResume();
    getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
  }

  @Override
  protected void onPause() {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
    sp.unregisterOnSharedPreferenceChangeListener(this);
    super.onPause();
  }

  public void networkToast() {
    Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
  }

  public void restoreActionBar() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setTitle(mTitle);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.my_stocks, menu);
    restoreActionBar();
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    if (id == R.id.action_change_units) {
      // this is for changing stock changes from percent value to dollar value
      Utils.showPercent = !Utils.showPercent;
      // todo curious about notifyChange here
      this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    Log.i(LOG_TAG, "onCreateLoader ran");

    // This narrows the return to only the stocks that are most current.
    return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
            new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                    QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
            QuoteColumns.ISCURRENT + " = ?",
            new String[]{"1"},
            null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    // todo look into swap cursor; curious about notifyChange in onOptionsItemSelected method
    mCursorAdapter.swapCursor(data);
    mCursor = data;
    updateEmptyView();
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mCursorAdapter.swapCursor(null);
  }


  private void updateEmptyView() {
    Log.i(LOG_TAG, "updateEmptyView getItemCount: " + mCursorAdapter.getItemCount());

    TextView tv = (TextView) findViewById(R.id.recycler_view_empty);
    RecyclerView rv = (RecyclerView) findViewById(R.id.recycler_view);
    rv.setPadding(0, 0, 0, 0);

    if (tv != null) {
      int message = R.string.empty_list;
      if (mCursorAdapter.getItemCount() == 0) {
        @StockTaskService.LocationStatus int location = Utils.getLocationStatus(mContext);
        switch (location) {
          case StockTaskService.LOCATION_STATUS_SERVER_DOWN:
            message = R.string.empty_list_server_down;
            break;
          case StockTaskService.LOCATION_STATUS_NON_EXISTENT_STOCK:
            // toast
            break;
          default:
            if (!Utils.isNetworkAvailable(this)) {
              message = R.string.empty_list_no_network;
            }
        }
        tv.setText(message);
      } else {
        if (!Utils.isNetworkAvailable(this)) {
          message = R.string.empty_list_not_updated;
          tv.setText(message);
          tv.setVisibility(View.VISIBLE);
          rv.setPadding(0, getPxFromDp(48), 0, 0);
        }
      }
    }
  }


  private int getPxFromDp(int dp) {
    Resources r = mContext.getResources();
    int px = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            r.getDisplayMetrics()
    );
    return px;
  }



  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.equals(getString(R.string.pref_location_status_key))) {
      updateEmptyView();
    }
  }
}
