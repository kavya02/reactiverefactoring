package myhomework.com.meilmanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import myhomework.com.meilmanager.database.MessageDatabaseHandler;

public class SettingActivity extends Activity {
    private ImageView mImgProfilePhoto;
    private TextView mTxtServerAddress, mTxtName, mTxtKeyPair;
    private Button logoutbtn;
    private Button regbtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initUI();
    }
    public void initUI() {
        mImgProfilePhoto = (ImageView)findViewById(R.id.imgProfilePhoto);

        mTxtServerAddress = (TextView) findViewById(R.id.txtServerAddress);

        logoutbtn = (Button) findViewById(R.id.logout_button);
        logoutbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final MessageDatabaseHandler msgDB = new MessageDatabaseHandler(getApplicationContext());
                msgDB.onDeleteAllData();

                ServerAPI.getInstance().logout(getSharedPreferences(
                        AppConstants.MY_PREFS_NAME, MODE_PRIVATE).getString("username", "CouldntRetrieveUsername"));

                Intent iter = new Intent(SettingActivity.this, LoginActivity.class);
                startActivity(iter);
                finish();
            }
        });

        regbtn = (Button) findViewById(R.id.register_button);
        regbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent iter = new Intent(SettingActivity.this, RegisterActivity.class);
                startActivity(iter);
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();

        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //Intent intent = new Intent(SettingActivity.this, MainActivity.class);
        //startActivity(intent);
        finish();
    }
}
