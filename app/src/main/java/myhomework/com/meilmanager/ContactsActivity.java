package myhomework.com.meilmanager;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import myhomework.com.meilmanager.Utils.GenericListener;
import myhomework.com.meilmanager.adapter.ContactListViewAdapter;
import myhomework.com.meilmanager.model.UserInfomation;
import myhomework.com.meilmanager.stages.GetChallengeStage;
import myhomework.com.meilmanager.stages.GetServerKeyStage;
import myhomework.com.meilmanager.stages.LogInStage;
import myhomework.com.meilmanager.stages.RegisterContactsStage;
import myhomework.com.meilmanager.stages.RegistrationStage;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

public class ContactsActivity extends Activity {

    private ImageView mImgPlus, mImgFilter;
    private ListView mListContacts;
//    private ServerAPI serverAPI = ServerAPI.getInstance();

    String server_name = "http://129.115.27.54:25666";

//    private Timer timer;
//    private RunRollTask roll;
//    private ContactDatabaseHandler userDB;

    Object mutex;
    Crypto myCrypto;

    private String username =  "kavyap";
    private ContactListViewAdapter adapter;

    ArrayList<UserInformation> contacts;
    ArrayList<String> contactNames;

//    public static List<UserInformation> contacts;
//    public ArrayList<String> contactNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        mutex = new Object();

//        SharedPreferences settings = getSharedPreferences(AppConstants.MY_PREFS_NAME, 0);
//        username = settings.getString("username", "UsernameNotSavedAfterLogin");
//
//        userDB = new ContactDatabaseHandler(this);
//        contacts = (ArrayList) userDB.getAllContactInfo();
//        adapter = new ContactListViewAdapter(this, (ArrayList) contacts);
//
//        contactNames = new ArrayList<>();
//        for(UserInfomation userInfo : contacts) {
//            contactNames.add(userInfo.getUserName());
//        }

        contacts = new ArrayList<>();
        contacts.add(new UserInformation("alice", false));
        contacts.add(new UserInformation("bob", false));

        contactNames = new ArrayList<>();
        for(UserInformation userInfo : contacts) {
            contactNames.add(userInfo.getUserName());
        }


        adapter = new ContactListViewAdapter(this, contacts, contactNames, mutex);

        initUI();

        myCrypto = new Crypto(getPreferences(Context.MODE_PRIVATE));



//        if(timer != null){
//            timer.cancel();
//        }
//        timer = new Timer();;
//        roll = new RunRollTask();
//        timer.schedule(roll, 3000, 3000);

        Observable.just(0) // the value doesn't matter, it just kicks things off
                .observeOn(Schedulers.newThread())
                .subscribeOn(Schedulers.newThread())
                .flatMap(new GetServerKeyStage(server_name))
                .flatMap(new RegistrationStage(server_name, username,
                        getBase64Image(), myCrypto.getPublicKeyString()))
                .flatMap(new GetChallengeStage(server_name,username,myCrypto))
                .flatMap(new LogInStage(server_name, username))
                .flatMap(new RegisterContactsStage(server_name, username, contactNames))
                .subscribe(new Observer<Notification>() {
                    @Override
                    public void onCompleted() {

                        // now that we have the initial state, start polling for updates

                        Observable.interval(0, 1, TimeUnit.SECONDS, Schedulers.newThread())
                                //   .take(5) // would only poll five times
                                //   .takeWhile( <predicate> ) // could stop based on a flag variable
                                .subscribe(new Observer<Long>() {
                                    @Override
                                    public void onCompleted() {
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                    }

                                    @Override
                                    public void onNext(Long numTicks) {
                                        Log.d("POLL", "Polling " + numTicks);
                                        checkForNotifications(contactNames);
                                    }
                                });
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("LOG", "Error: ", e);
                    }

                    @Override
                    public void onNext(Notification notification) {
                        // handle initial state here
//                        Log.d("LOG", "Next " + notification);
//                        if (notification instanceof Notification
//                                .LogIn) {
//                            Log.d("LOG", "User " + ((Notification.LogIn) notification).username + " is logged in");
//                        }
//                        if (notification instanceof Notification.LogOut) {
//                            Log.d("LOG", "User " + ((Notification.LogOut) notification).username + " is logged out");
//                        }
                    }
                });

    }
    String getBase64Image(){
        InputStream is;
        byte[] buffer = new byte[0];
        try {
            is = getAssets().open("images/ic_android_black_24dp.png");
            buffer = new byte[is.available()];
            is.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Base64.encodeToString(buffer,Base64.DEFAULT).trim();
    }

    private void checkForNotifications(ArrayList<String> contactNames) {
        try {
            JSONObject json = new JSONObject();
            json.put("username", username);
            json.put("friends", new JSONArray(contactNames));
            JSONObject response = WebHelper.JSONPut(server_name + "/register-friends", json);

            JSONObject status = response.getJSONObject("friend-status-map");

            for (String contact : contactNames) {
                Log.d("POLL RESULTS ", contact + " : " + status.getString(contact));
            }

            synchronized (mutex) {
                for (UserInformation userInfo : contacts) {
                    String onlineStatus = status.getString(userInfo.getUserName());

                    if (onlineStatus == null) {
                        Log.d("UPDATE CONTACTS STATUS ", "Can't get status for <" + userInfo.getUserName() + ">");
                    } else {
                        if (onlineStatus.equals("logged-in")) {
                            userInfo.setIsOnline(true);
                        } else {
                            userInfo.setIsOnline(false);
                        }
                    }
                }
            }

            // Update changes in ListView
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();

                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    public void initUI() {
        mImgPlus = (ImageView) findViewById(R.id.imgPlus);
        mImgPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent iner = new Intent(ContactsActivity.this, ContactActivity.class);
                startActivity(iner);
            }
        });
        mImgFilter = (ImageView) findViewById(R.id.imgFilter);

        mListContacts = (ListView) findViewById(R.id.listContact);
        mListContacts.setAdapter(adapter);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }


}
