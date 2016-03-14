package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.sam_chordas.android.stockhawk.R;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_activity);
//        getActionBar().setDisplayHomeAsUpEnabled(true);

        // add the about fragment
        if (savedInstanceState == null) {

//            Bundle arguments = new Bundle();
//            arguments.putParcelable("symbol", getIntent().getExtras());
//            Log.i("LOG_TAG", "get extras: " + getIntent().getExtras());

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(getIntent().getExtras());

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }


    }



}
