package myhomework.com.meilmanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by root on 23.07.2016.
 */
public class LoginActivity extends Activity {
    private EditText usernameEditBox;
    private Button loginButton;
    private TextView signupLink;

    private ServerAPI serverAPI = ServerAPI.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initUI();
    }

    public void initUI() {
        usernameEditBox = (EditText) findViewById(R.id.input_username);

        loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serverAPI.login(usernameEditBox.getText().toString());

                if (usernameEditBox.getText().toString().equals("sreddy")){

                    Intent iter = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(iter);

                }else {
                    Toast.makeText(getApplicationContext(), "Login Failed", Toast.LENGTH_LONG).show();
                }

            }
        });

        signupLink = (TextView) findViewById(R.id.link_signup);
        signupLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();

        finish();
    }
}
