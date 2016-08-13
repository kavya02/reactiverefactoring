package myhomework.com.meilmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import myhomework.com.meilmanager.Utils.GenericListener;
import myhomework.com.meilmanager.database.ContactDatabaseHandler;
import myhomework.com.meilmanager.model.EmailData;
import myhomework.com.meilmanager.model.UserInfomation;

public class ContactActivity extends Activity {

    Activity mContext;

    private ImageView mImgProfilePhoto, mImgDelete, mImgSearch;
    private EditText mTxtName, mTxtPublicKey;
    private Button mBtnSave;

    int REQUEST_CAMERA = 10, SELECT_FILE = 11;
    Uri mImgUri = null;

    private ServerAPI serverAPI = ServerAPI.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);
        mContext = this;

        initUI();
    }

    public void initUI() {
        mImgProfilePhoto = (ImageView)findViewById(R.id.imgProfilePhoto);

        mImgProfilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseAvatar();
            }
        });
        mImgDelete = (ImageView) findViewById(R.id.imgDelete);
        mImgDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "Data discarded!", Toast.LENGTH_SHORT).show();
                //Intent iner = new Intent(ContactActivity.this, ContactsActivity.class);
                //startActivity(iner);
                finish();
            }
        });

        mTxtName = (EditText) findViewById(R.id.etxtFilter);
        mTxtPublicKey = (EditText) findViewById(R.id.txtPublicKey);

        mImgSearch = (ImageView) findViewById(R.id.imgFilter);
        mImgSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serverAPI.getUserInfo(mTxtName.getText().toString(), new GenericListener<ServerAPI.UserInfo>() {
                    @Override
                    public void onResponse(ServerAPI.UserInfo info) {
                        processUserInfo(info);
                    }

                    @Override
                    public void onError(String errorMessage) {

                    }
                });
            }
        });

        mBtnSave = (Button) findViewById(R.id.btnSave);
        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTxtName.getText().length() > 0
                        && mImgUri != null
                        && mTxtPublicKey.getText().length() > 0) {
                    final ContactDatabaseHandler contactDB = new ContactDatabaseHandler(mContext);
                    contactDB.AddContactInfo(new UserInfomation("userId", mTxtName.getText().toString(), mImgUri.toString(), mTxtPublicKey.getText().toString()));
                    //Intent iner = new Intent(ContactActivity.this, ContactsActivity.class);
                    //startActivity(iner);
                    Toast.makeText(mContext, "Contact saved", Toast.LENGTH_SHORT).show();
                    finish();

                } else {
                    new AlertDialog.Builder(v.getContext())
                            .setTitle("Invalid Info")
                            .setMessage("Please check again!")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                                    return;
                                }
                            }).create().show();
                }

            }
        });
    }

    /*
    * This Function converts the String back to Bitmap
    * */
    private Bitmap getBitmapFromString(String encodedImage) {
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        return decodedByte;
    }

    void processUserInfo(ServerAPI.UserInfo userInfo){
        mTxtPublicKey.setText(userInfo.publicKey);
        mImgProfilePhoto.setImageBitmap(getBitmapFromString(userInfo.image));
    }

    private void chooseAvatar() {
        final CharSequence[] items = {getString(R.string.str_take_photo),getString(R.string.str_load_from_gallery),
                getString(R.string.str_cancel) };

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.str_choose_profile_photo);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals(getString(R.string.str_take_photo))) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, REQUEST_CAMERA);
                } else if (items[item].equals(getString(R.string.str_load_from_gallery))) {
                    Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, SELECT_FILE);
                } else if (items[item].equals(getString(R.string.str_cancel))) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }
    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
    public Bitmap getScaledThumbBitmapFromBitmap(Bitmap bmImage) {
        int wid = bmImage.getWidth();
        int hei = bmImage.getHeight();
        int showWidHei = getResources().getDimensionPixelSize(R.dimen.pad_90dp);
        float nScale = showWidHei / (float)wid;
        Bitmap bmRet = ThumbnailUtils.extractThumbnail(bmImage, (int) (wid * nScale), (int) (hei * nScale));
        return bmRet;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //Intent iner = new Intent(ContactActivity.this, ContactsActivity.class);
        //startActivity(iner);
        finish();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if(requestCode == REQUEST_CAMERA || requestCode == SELECT_FILE) {
                if (data == null) return;
                if (requestCode == REQUEST_CAMERA) {
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    mImgUri = getImageUri(mContext, photo);
                } else if (requestCode == SELECT_FILE) {
                    mImgUri = data.getData();
                }
                Bitmap bmAvatar = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(),mImgUri);
                Bitmap bmNew = getScaledThumbBitmapFromBitmap(bmAvatar);
                mImgProfilePhoto.setImageBitmap(bmNew);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

}
