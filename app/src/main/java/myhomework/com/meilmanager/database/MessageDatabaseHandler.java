package myhomework.com.meilmanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import myhomework.com.meilmanager.model.EmailData;


public class MessageDatabaseHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;


    private static final String DATABASE_NAME = "MessageDB";

    public static final String TABLE_MESSAGE = "MessageTable";

    public static final String COLUMN_id = "id";

    public static final String COLUMN_SENDER = "Sender";
    public static final String COLUMN_SUBJECT = "Subject";
    public static final String COLUMN_BODY = "Body";
    public static final String COLUMN_TIME = "Time";



    public MessageDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String createEnquiry = "CREATE TABLE "
                + TABLE_MESSAGE + "("
                + COLUMN_id  + " integer primary key autoincrement, "
                + COLUMN_SENDER + " text, "
                + COLUMN_SUBJECT + " text, "
                + COLUMN_BODY + " text, "
                + COLUMN_TIME + " text"
                + " )";
        db.execSQL(createEnquiry);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGE);
        onCreate(db);
    }

    public void onDeleteAllData(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGE);
        onCreate(db);
    }

    public void AddEmailInfo(EmailData emailData) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_SENDER, emailData.getSender());
        values.put(COLUMN_SUBJECT, emailData.getSubject());
        values.put(COLUMN_BODY, emailData.getBody());
        values.put(COLUMN_TIME, emailData.getTime());

        db.insert(TABLE_MESSAGE, null, values);
        db.close();
    }

    public EmailData getEmailItem(String emailID){
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_MESSAGE,
                new String[]{
                        COLUMN_id,
                        COLUMN_SENDER,
                        COLUMN_SUBJECT,
                        COLUMN_BODY,
                        COLUMN_TIME},
                COLUMN_id + "=?",
                new String[]{emailID}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        EmailData contact = new EmailData(
                cursor.getString(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4)
        );
        return contact;
    }

    public void UpdateEmailInfo(EmailData emailData) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_id, emailData.getEmailID());
        values.put(COLUMN_SENDER, emailData.getSender());
        values.put(COLUMN_SUBJECT, emailData.getSubject());
        values.put(COLUMN_BODY, emailData.getBody());
        values.put(COLUMN_TIME, emailData.getTime());
        db.update(TABLE_MESSAGE, values, COLUMN_id + "='" + emailData.getEmailID() + "'", null);
        db.close();
    }

    public void DeleteEmailInfo(String emailID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGE, COLUMN_id + "='" + emailID + "'", null);
    }
    public List<EmailData> getAllEmailInfo() {
        List<EmailData> contactList = new ArrayList<EmailData>();
        String selectQuery = "SELECT  * FROM " + TABLE_MESSAGE;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                EmailData qu = new EmailData();
                qu.setEmailID(cursor.getString(0));
                qu.setSender(cursor.getString(1));
                qu.setSubject(cursor.getString(2));
                qu.setBody(cursor.getString(3));
                qu.setTime(cursor.getString(4));
                contactList.add(qu);
            } while (cursor.moveToNext());
        }
        return contactList;
    }
}