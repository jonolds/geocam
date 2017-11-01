package com.jonolds.geocam;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

public class MarkerProvider extends ContentProvider {
    private static String LOGTAG = "ToDoProvider:";//LOGTAG set to CLass Name
    private static final String DBNAME = "MarkerDB";// Database Name for SQLITE DB
    private static final String AUTHORITY = "com.jonolds.geocam.markerprovider";//Authority: pack name
    private static final String TABLE_NAME = "Markers";//TABLE_NAME: defined as ToDoList
    //Create a CONTENT_URI for use by other classes
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/"+TABLE_NAME);
    //Column names for the ToDoList Table
    public static final String TODO_TABLE_COL_ID = "_ID";
    public static final String TODO_TABLE_COL_TITLE = "TITLE";
    public static final String TODO_TABLE_COL_LATITUDE = "LATITUDE";
    public static final String TODO_TABLE_COL_LONGITUDE = "LONGITUDE";
    public static final String TODO_TABLE_COL_ADDRESS = "ADDRESS";
 //   public static final String TODO_TABLE_COL_TIME = "TIME";

    //Table create string based on column names
    private static final String SQL_CREATE_MAIN = "CREATE TABLE " +
            TABLE_NAME+ " " +                       // Table's name
            "(" +                           // The columns in the table
            TODO_TABLE_COL_ID + " INTEGER PRIMARY KEY, " +
            TODO_TABLE_COL_TITLE + " TEXT," +
            TODO_TABLE_COL_LATITUDE + " REAL," +
            TODO_TABLE_COL_LONGITUDE + " REAL," +
            TODO_TABLE_COL_ADDRESS + " TEXT)";

    //URI Matcher object to facilitate switch cases between URIs
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private MainDatabaseHelper mOpenHelper;

    //Constructor adds two URIs, use for case statements
    public MarkerProvider() {
        sUriMatcher.addURI(AUTHORITY,TABLE_NAME,1); //Match to the authority and the table name, assign 1
        sUriMatcher.addURI(AUTHORITY,TABLE_NAME+"/#",2);//Match to the authority and the table name, and an ID, assign 2
    }

    //Query Functionality for the Content Provider, Pass either the table name, or the table name with an ID
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        //Use an SQLiteQueryBuilder object to create the query
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        //Set the table to be queried
        queryBuilder.setTables(TABLE_NAME);
        //Match on either the URI with or without an ID
        switch (sUriMatcher.match(uri)){
            case 1:
                //If no ID, and no sort order, specify the sort order as by ID Ascending
                if(TextUtils.isEmpty(sortOrder)) sortOrder="_ID ASC";
                break;
            case 2:
                //Otherwise, set the selection of the URI to the ID
                selection = selection + "_ID = " + uri.getLastPathSegment();
                break;
            default:
                Log.e(LOGTAG, "URI not recognized " + uri);
        }
        //Query the database based on the columns to be returned, the selection criteria and
        // arguments, and the sort order
        return queryBuilder.query(mOpenHelper.getWritableDatabase(),projection,selection,
                selectionArgs,null,null,sortOrder);

    }

    //Insert functionality of the Content Provider, Pass ContentValues object w/ values to be inserted
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        switch (sUriMatcher.match(uri)){
            //Match against a URI with just the table name
            case 1:
                break;
            default:
                //Otherwise, error is thrown
                Log.e(LOGTAG, "URI not recognized " + uri);
        }
        //Insert into the table, return the id of the inserted row
        long id = mOpenHelper.getWritableDatabase().insert(TABLE_NAME,null,values);
        nap();//Sleep for .5 seconds
        //Notify context of change
        getContext().getContentResolver().notifyChange(uri,null);
        //Return the URI with the ID at the end
        return Uri.parse(CONTENT_URI+"/" + id);
    }

    //Update functionality for the Content Provider
    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        switch (sUriMatcher.match(uri)){
            case 1:
                //Allow update based on multiple selections
                break;
            case 2:
                //Allow updates based on a single ID
                String id = uri.getPathSegments().get(1);
                selection = TODO_TABLE_COL_ID + "=" + id +
                        (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "");
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        //Perform updates and return the number that were updated
        int updateCount = mOpenHelper.getWritableDatabase().update(TABLE_NAME,values,
                selection,selectionArgs);
        nap();//Sleep for .5 seconds
        //Notify the context
        getContext().getContentResolver().notifyChange(uri,null);
        //Return the number of rows updated
        return updateCount;
    }

    //Delete functionality for Content Provider. Pass the URI for the table and the ID to be deleted
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)){
            //Match on URI with ID
            case 2:
                String id = uri.getPathSegments().get(1);
                selection = TODO_TABLE_COL_ID + "=" + id +
                        (!TextUtils.isEmpty(selection) ? "AND (" + selection  + ")" : "");
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);//Else, error is thrown
        }
        //Delete from the database, return integer value for how many rows are deleted
        int deleteCount = mOpenHelper.getWritableDatabase().delete(TABLE_NAME,selection,selectionArgs);
        nap();//Sleep for .5 seconds
        //Notify calling context
        getContext().getContentResolver().notifyChange(uri,null);
        //Return number of rows deleted (if any)
        return deleteCount;
    }

    //Create Content Provider, Check if db/table exist. If not: create table, All implemented: MainDatabaseHelper Class
    @Override
    public boolean onCreate() {
        /*
         * Creates a new helper object. This method always returns quickly.
         * Notice that the database itself isn't created or opened
         * until SQLiteOpenHelper.getWritableDatabase is called
         */
        mOpenHelper = new MainDatabaseHelper(getContext());
        return true;
    }

    //Class for creating an instance of a SQLiteOpenHelper. Performs creation of the SQLite Database if none exists
    private static final class MainDatabaseHelper extends SQLiteOpenHelper {

        // Instantiates an open helper for the provider's SQLite data repository
        // Do not do database creation and upgrade here.
        MainDatabaseHelper(Context context) {
            super(context, DBNAME, null, 1);
        }

        //Creates the data repository. This is called when the provider attempts to open the
        // repository and SQLite reports that it doesn't exist.
        public void onCreate(SQLiteDatabase db) {
            // Creates the main table
            db.execSQL(SQL_CREATE_MAIN);
        }

        public void onUpgrade(SQLiteDatabase db, int int1, int int2){
            db.execSQL("DROP TABLE IF EXISTS ToDoList");
            onCreate(db);
        }
    }

    public void nap() {//Sleep for .5 seconds to emulate network latency
        try {Thread.sleep(500);}
        catch (java.lang.InterruptedException myEx){Log.e(LOGTAG,myEx.toString());}
    }
    @Override//Not implemented. Would return the MIME type requests
    public String getType(@NonNull Uri uri) {return "N/A";}
}