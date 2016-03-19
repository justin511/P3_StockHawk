package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

/**
 * Created by sam_chordas on 10/5/15.
 */
@Database(version = QuoteDatabase.VERSION)
public class QuoteDatabase {
  private QuoteDatabase(){}

  public static final int VERSION = 9;

  @Table(QuoteColumns.class) public static final String QUOTES = "quotes";

  @Table(HistoryColumns.class) public static final String HISTORY = "history";



//  @OnUpgrade
//  public static void onUpgrade(Context context, SQLiteDatabase db, int oldVersion,
//                                          int newVersion) {
//    db.execSQL("DROP TABLE IF EXISTS " + QuoteDatabase.QUOTES);
//    db.execSQL("DROP TABLE IF EXISTS " + QuoteDatabase.HISTORY);
//    onCreate(context, db);
//  }

//  @OnCreate public static void onCreate(Context context, SQLiteDatabase sqLiteDatabase) {
//
//  }
}
