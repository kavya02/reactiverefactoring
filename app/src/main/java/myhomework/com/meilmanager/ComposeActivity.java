package myhomework.com.meilmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import myhomework.com.meilmanager.adapter.EmailListViewAdapter;
import myhomework.com.meilmanager.database.ContactDatabaseHandler;
import myhomework.com.meilmanager.database.MessageDatabaseHandler;
import myhomework.com.meilmanager.model.EmailData;
import myhomework.com.meilmanager.model.UserInfomation;

public class ComposeActivity extends Activity {
    private ImageView mImgDelete, mImgProcess;
    private Button mBtnSent;
    private EditText mETxtToEmail, mETxtSubject, mETxtBody;
    Context mContext;
    final ContactDatabaseHandler userDB = new ContactDatabaseHandler(this);


    private ServerAPI serverAPI = ServerAPI.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        mContext = this;
        initUI();
        SetName();
    }

    public void SetName() {
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            return;
        }
        String strName = extras.getString("Name");
        if (!strName.equals(null)){
            mETxtToEmail.setText(strName);
        }

    }

    public void initUI() {
        mImgDelete = (ImageView)findViewById(R.id.imgDelete);
        mImgDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent iner = new Intent(ComposeActivity.this, MainActivity.class);
                //startActivity(iner);
                Toast.makeText(mContext, "Message discarded!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        mImgProcess = (ImageView) findViewById(R.id.imgProcess);

        mBtnSent = (Button) findViewById(R.id.buttonSent);
        mBtnSent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mETxtToEmail.getText().length() >0
                        && mETxtBody.getText().length() >0
                        && mETxtSubject.getText().length()>0){
                    final MessageDatabaseHandler messageDB = new MessageDatabaseHandler(v.getContext());
                    messageDB.AddEmailInfo(new EmailData("",
                            mETxtToEmail.getText().toString(),
                            mETxtSubject.getText().toString(),
                            mETxtBody.getText().toString(),
                            "" + System.currentTimeMillis()));


                    SharedPreferences settings = getSharedPreferences(AppConstants.MY_PREFS_NAME, 0);
                    String username = settings.getString("username", "UsernameNotSavedAfterLogin");

                    List<UserInfomation> userInfos = userDB.getAllContactInfo();
                    String pkey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArB0Z1ogGvaGarvR8d+GB4tpThYJ/HM5o\nUoCeWkAKtHL30yN5yWfddacKp34T2UY7Zmz/mwdBtcJHieG7pR3fTARWz3xLjVIw7tVg1Cui7mNH\nMwvSyVMdEBsCunPivl7n2fnoIqqD6UAIvZdemZgRBbubNraiANP2vsLqw85KdGFQrvNCpqVAPfom\nvECfOlIywOfsiL9z6X2LhyAhCGKDO6s2rsJWWhIART0PHAYysvUnogWMPSTn8wyN1XLo5w/X2vS6\n62HWsJmKIa3oDyARNGhpevMWrzRgYImD66vO6edMaxGa3eCqOz3dX5OlRmSUSu8kNhUf5U/ksG+n\n6UgDIwIDAQAB\n";
                    for(UserInfomation uinf : userInfos) {
                        //System.out.println(uinf.getUserName());
                        if(uinf.getUserName().equals(mETxtToEmail.getText().toString())) {
                            pkey = uinf.getPublicKey();
                            //System.out.println("FOUND!" + uinf.getUserName() + " pk: " + uinf.getPublicKey());
                        }
                    }

                    /*String pk = userDB.getContactItem(username).getPublicKey();*/

                    serverAPI.sendMessage(new Object(),
                            Crypto.getPublicKeyFromString(pkey),
                            username,
                            mETxtToEmail.getText().toString(),
                            mETxtSubject.getText().toString(),
                            mETxtBody.getText().toString(),
                            System.currentTimeMillis(),
                            60l);

                    Toast.makeText(v.getContext(), "Sending message ...", Toast.LENGTH_SHORT).show();



                    //Intent iner = new Intent(ComposeActivity.this, MainActivity.class);
                    //startActivity(iner);
                    finish();
                } else {
                    new AlertDialog.Builder(v.getContext())
                            .setTitle("Invalid Info")
                            .setMessage("Please type again!")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                                    return;
                                }
                            }).create().show();
                }
            }
        });

        mETxtToEmail = (EditText) findViewById(R.id.etxtToEmail);
        mETxtToEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent iner = new Intent(ComposeActivity.this, ContactsActivity.class);
                startActivity(iner);
                finish();
            }
        });
        mETxtSubject = (EditText) findViewById(R.id.etxtSubject);
        mETxtBody    = (EditText) findViewById(R.id.etxtBody);


    }
}
