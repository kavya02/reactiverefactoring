package myhomework.com.meilmanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;


/**
 * Created by root on 23.07.2016.
 */
public class RegisterActivity extends Activity {
    private EditText usernameEditBox;
    private Button registerButton;
    private TextView loginLink;

    private ServerAPI serverAPI = ServerAPI.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initUI();
    }

    public void initUI() {
        usernameEditBox = (EditText) findViewById(R.id.input_register_username);

        registerButton = (Button) findViewById(R.id.signup_button);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                InputStream is;
                byte[] buffer = new byte[0];
                try {
                    is = getAssets().open("images/ic_android_black_24dp.png");
                    buffer = new byte[is.available()];
                    is.read(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String username = usernameEditBox.getText().toString();
                serverAPI.register(username, Base64.encodeToString(buffer, Base64.DEFAULT).trim());
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);

            }
        });

        loginLink = (TextView) findViewById(R.id.link_login);
        loginLink.setClickable(true);
        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        finish();
    }
}
