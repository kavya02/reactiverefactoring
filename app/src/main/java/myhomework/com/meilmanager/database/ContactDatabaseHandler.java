package myhomework.com.meilmanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import myhomework.com.meilmanager.model.UserInfomation;


public class ContactDatabaseHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;


    private static final String DATABASE_NAME = "ContactDB";

    public static final String TABLE_CONTACT = "ContactTable";

    public static final String COLUMN_id = "id";

    public static final String COLUMN_USERNAME = "UserName";
    public static final String COLUMN_USERIMAGE = "UserImage";
    public static final String COLUMN_PUBLICKEY = "PublicKey";



    public ContactDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String createEnquiry = "CREATE TABLE "
                + TABLE_CONTACT + "("
                + COLUMN_id  + " integer primary key autoincrement, "
                + COLUMN_USERNAME + " text, "
                + COLUMN_USERIMAGE + " text, "
                + COLUMN_PUBLICKEY + " text"
                + " )";
        db.execSQL(createEnquiry);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACT);
        onCreate(db);
    }

    public void onDeleteAllData(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACT);
        onCreate(db);
    }

    public void AddContactInfo(UserInfomation contactInfo) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, contactInfo.getUserName());
        values.put(COLUMN_USERIMAGE, contactInfo.getUserImage());
        values.put(COLUMN_PUBLICKEY, contactInfo.getPublicKey());

        db.insert(TABLE_CONTACT, null, values);
        db.close();
    }

    public UserInfomation getContactItem(String studentEmail){
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_CONTACT,
                new String[]{
                        COLUMN_id,
                        COLUMN_USERNAME,
                        COLUMN_USERIMAGE,
                        COLUMN_PUBLICKEY},
                COLUMN_USERNAME + "=?",
                new String[]{studentEmail}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        UserInfomation contact = new UserInfomation(
                cursor.getString(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3)
        );
        return contact;
    }

    public void UpdateContactInfo(UserInfomation contactInfo) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_id, contactInfo.getUserID());
        values.put(COLUMN_USERNAME, contactInfo.getUserName());
        values.put(COLUMN_USERIMAGE, contactInfo.getUserImage());
        values.put(COLUMN_PUBLICKEY, contactInfo.getPublicKey());
        db.update(TABLE_CONTACT, values, COLUMN_id + "='" + contactInfo.getUserID() + "'", null);
        db.close();
    }

    public void DeleteContactInfo(String contactID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CONTACT, COLUMN_id + "='" + contactID + "'", null);
    }
    public List<UserInfomation> getAllContactInfo() {
        List<UserInfomation> contactList = new ArrayList<UserInfomation>();
        String selectQuery = "SELECT  * FROM " + TABLE_CONTACT;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                UserInfomation qu = new UserInfomation();
                qu.setUserID(cursor.getString(0));
                qu.setUserName(cursor.getString(1));
                qu.setUserImage(cursor.getString(2));
                qu.setPublicKey(cursor.getString(3));
                contactList.add(qu);
            } while (cursor.moveToNext());
        }
        return contactList;
    }
}