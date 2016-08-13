package myhomework.com.meilmanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.SyncStateContract;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.crypto.SecretKey;

import myhomework.com.meilmanager.Utils.GenericListener;
import myhomework.com.meilmanager.model.EmailData;

/**
 * Created by kbaldor on 7/4/16.
 */
public class ServerAPI {
    private final String API_VERSION = "0.4.0";
    private static String LOG       = "ServerAPI";
    private String myServerName = "129.115.27.54";
    private String myServerPort = "25666";

    Context appContext;

    private ArrayList<EmailData> lastMessages = new ArrayList<>();

    private GenericListener<ArrayList<EmailData>> emailsListener;

    private static ServerAPI ourInstance;
    Crypto myCrypto;

    PublicKey serverKey=null;

    RequestQueue commandQueue;
    RequestQueue pseudoPushQueue;

    public static ServerAPI getInstance(Context context) {

        if(ourInstance==null){
            ourInstance = new ServerAPI(context);
        }
        return ourInstance;
    }

    public static ServerAPI getInstance() {
        if (null == ourInstance)
        {
            throw new IllegalStateException(ServerAPI.class.getSimpleName() +
                    " is not initialized, call getInstance(...) first");
        }
        return ourInstance;
    }

    private ServerAPI(Context context) {
        appContext = context;
        commandQueue = Volley.newRequestQueue(context);
        pseudoPushQueue = Volley.newRequestQueue(context);
        myCrypto = new Crypto(
                context.getSharedPreferences(AppConstants.MY_PREFS_NAME, Context.MODE_PRIVATE));


        registerListener();

        checkAPIVersion();
        getServerAddress(myServerName);
    }

    private String makeURL(String... args){
        return "http://" + myServerName + ":" + myServerPort + "/" + TextUtils.join("/",args);
    }

    private void getServerAddress(final String servername){
        (new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    Log.d(LOG,"Address is: " + InetAddress.getByName(servername).getHostAddress());
                    getStringCommand(makeURL("get-key"),
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String key) {
                                    serverKey = Crypto.getPublicKeyFromString(key);
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.d(LOG, "Couldn't get key", error);
                                }
                            });

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    public void setServerPort(final String serverPort){
        myServerPort = serverPort;
    }

    public void setServerName(final String serverName){

        myServerName = serverName;
        getServerAddress(serverName);
    }

    public void checkAPIVersion() {
        getStringCommand(makeURL("api-version"),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        if (s.equals(API_VERSION)) sendGoodAPIVersion();
                        else sendBadAPIVersion();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        sendCommandFailed("checkAPIVersion", error);
                    }
                });
    }

    public void register(final String username, String image) {
        putJSONCommand(makeURL("register"), keyValuePairs("username", username,
                        "image", image,
                        "public-key", myCrypto.getPublicKeyString()),
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        handleRegisterResponse(response);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        sendCommandFailed("register", error);
                    }
                });

    }

    private void handleRegisterResponse(JSONObject response){
        try {
            Log.d(LOG,"Response: status: " + response.getString("status") +
                               " reason: " + response.getString("reason"));
            if(response.getString("status").equals("ok"))
                sendRegistrationSucceeded();
            else
                sendRegistrationFailed(response.getString("reason"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void getUserInfo(final String username, final GenericListener<UserInfo> volleyListener){
        String url = makeURL("get-contact-info",username);
        Log.d(LOG,"getting user info with "+url);

        getJSONCommand(makeURL("get-contact-info",username),
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String status = response.getString("status");
                            if(status.equals("ok")) {
                                UserInfo userInfo = new UserInfo(response.getString("username"),
                                        response.getString("image"),
                                        response.getString("key"));
                                volleyListener.onResponse(userInfo);
                                sendUserInfo(userInfo);
                            } else {
                                sendUserNotFound(username);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Log.d(LOG,"Response: " + response);
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                sendCommandFailed("getUserInfo",error);
            }
        });
    }

    public void login(final String username) {
        if(serverKey != null) {
            getStringCommand(makeURL("get-challenge", username),
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String challenge) {
                            processChallengeAndLogin(challenge, username, myCrypto);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            sendCommandFailed("login", error);
                        }
                    });
        } else {
            sendLoginFailed("server key was null");
        }
    }


    private void processChallengeAndLogin(final String challenge, final String username,final Crypto crypto) {
        try {
            byte[] decrypted = crypto.decryptRSA(Base64.decode(challenge, Base64.NO_WRAP));
            String response = Base64.encodeToString(Crypto.encryptRSA(decrypted, serverKey), Base64.NO_WRAP);

            putJSONCommand(makeURL("login"), keyValuePairs("username",username,
                            "response",response),
                    new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            handleLoginResponse(username, response);
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            sendCommandFailed("login",error);
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            sendLoginFailed("user-not-registered");
        }


    }

    private void handleLoginResponse(String username, JSONObject response){
        try {
            if(response.getString("status").equals("ok"))
                sendLoginSucceeded(username);
            else
                sendLoginFailed(response.getString("reason"));
        } catch (JSONException e) {
            e.printStackTrace();
            sendLoginFailed("unable to parse JSON response");
        }
    }


    public void logout(final String username) {
        if(serverKey != null) {
            getStringCommand(makeURL("get-challenge", username),
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String challenge) {
                            processChallengeAndLogout(challenge, username, myCrypto);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            sendCommandFailed("logout", error);
                        }
                    });
        } else {
            sendLogoutFailed("server key was null");
        }
    }


    private void processChallengeAndLogout(final String challenge, final String username,final Crypto crypto) {
        byte[] decrypted = crypto.decryptRSA(Base64.decode(challenge, Base64.NO_WRAP));
        String response = Base64.encodeToString(Crypto.encryptRSA(decrypted,serverKey),Base64.NO_WRAP);

        putJSONCommand(makeURL("logout"), keyValuePairs("username",username,
                                                        "response",response),
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        handleLogoutResponse(response);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        sendCommandFailed("logout",error);
                    }
                });
    }

    private void handleLogoutResponse(JSONObject response){
        try {
            if(response.getString("status").equals("ok"))
                sendLogoutSucceeded();
            else
                sendLogoutFailed(response.getString("reason"));
        } catch (JSONException e) {
            e.printStackTrace();
            sendLogoutFailed("unable to parse JSON response");
        }
    }

    public void registerContacts(String username, ArrayList<String> names, final GenericListener<HashMap<String, String>> volleyListener){
        final JSONObject json = new JSONObject();
        try {
            json.put("username",username);
            json.put("friends",new JSONArray(names));
            putJSONCommand(makeURL("register-friends"), json,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            Log.d(LOG, "register friends response " + jsonObject);
                            handleRegisterFriendsResponse(jsonObject, volleyListener);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            Log.d(LOG, "register friends error", volleyError);
                        }
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private HashMap<String, String> handleRegisterFriendsResponse(JSONObject response, final GenericListener<HashMap<String, String>> volleyListener) {
        HashMap<String, String> loginStates = new HashMap<>();

        try {
            JSONObject friend_status = response.getJSONObject("friend-status-map");
            Iterator<String> it = friend_status.keys();

            while (it.hasNext()) {
                String friendName = it.next();
                loginStates.put(friendName, friend_status.getString(friendName));
            }

            volleyListener.onResponse(loginStates);

        } catch( JSONException e) {
            e.printStackTrace();
        }

        sendRegisterContactsResponse(loginStates);

        return loginStates;
    }


    /*
     * TODO: This currently only supports polling
     */
    public void startPushListener(final String username, final GenericListener<ArrayList<EmailData>> volleyListn){
        emailsListener = volleyListn;
        String url = makeURL("wait-for-push",username);
        Log.d(LOG,"waiting for push with "+url);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(LOG, response.toString());
                        handleNotifications(response);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                sendCommandFailed("pushListener",error);
            }
        });
        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(
                60000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        pseudoPushQueue.add(jsObjRequest);
    }

    private void handleNotification(JSONObject notification){
        try {
            String type = notification.getString("type");
            if(type.equals("login")){
                sendContactLogin(notification.getString("username"));
            }
            if(type.equals("logout")){
                sendContactLogout(notification.getString("username"));
            }
            if(type.equals("message")){
                System.out.println(notification.getString("content"));
                handleMessage(new JSONObject(notification.getString("content")));//.replace("\\\"", "")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String decryptAES64ToString(String aes64, SecretKey aesKey) throws UnsupportedEncodingException {
        byte[] bytes = Base64.decode(aes64,Base64.NO_WRAP);
        if(bytes==null) return null;
        bytes = Crypto.decryptAES(bytes, aesKey);
        if(bytes==null) return null;
        return new String(bytes,"UTF-8");
    }

    private void handleMessage(JSONObject message){
        Log.d(LOG, message.toString());
        try{
            SecretKey aesKey = Crypto.getAESSecretKeyFromBytes(myCrypto.decryptRSA(Base64.decode(message.getString("aes-key"),Base64.NO_WRAP)));
            String sender = decryptAES64ToString(message.getString("sender"),aesKey);
            String recipient = decryptAES64ToString(message.getString("recipient"),aesKey);
            String body = decryptAES64ToString(message.getString("body"),aesKey);
            String subject = decryptAES64ToString(message.getString("subject-line"),aesKey);
            Long born = Long.parseLong(decryptAES64ToString(message.getString("born-on-date"),aesKey));
            Long ttl = Long.parseLong(decryptAES64ToString(message.getString("time-to-live"),aesKey));
            Log.d(LOG,sender+" says:");
            Log.d(LOG,subject+":");
            Log.d(LOG, body);
            Log.d(LOG,"ttl: "+ttl);
            sendMessageDelivered(sender, recipient, subject, body, born, ttl);

                Log.d(LOG, "RECEIEVED MESSAGE: \n\n" + new EmailData("id", sender, subject, body, ttl.toString()).toString());
            lastMessages.add(new EmailData("id", sender, subject, body, ttl.toString()));
        } catch (Exception e) {
            Log.d(LOG,"Failed to parse message",e);
        }


    }

    private void handleNotifications(JSONObject notifications){
        lastMessages.clear();

        try {
            JSONArray array = notifications.getJSONArray("notifications");
            for(int index = 0; index < array.length(); index++){
                handleNotification(array.getJSONObject(index));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        emailsListener.onResponse(lastMessages);
        sendLastRecievedMessages(lastMessages);
    }

    private String base64AESEncrypted(String clearText, SecretKey aesKey){
        try {
            return Base64.encodeToString(Crypto.encryptAES(clearText.getBytes("UTF-8"),aesKey), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    /*
     * The messageReference can be any object. It is used to keep track of which messages
     * succeeded or failed
     */
    public void sendMessage(final Object messageReference,
                            PublicKey recipientKey,
                            String sender,
                            String recipient,
                            String subjectLine,
                            String body,
                            Long bornOnDate,
                            Long timeToLive) {
        SecretKey aesKey = Crypto.createAESKey();
        byte[] aesKeyBytes = aesKey.getEncoded();
        if(aesKeyBytes==null){
            Log.d(LOG,"AES key failed (this should never happen)");
            sendSendMessageFailed(messageReference,"AES key failed");
            return;
        }
        String base64encryptedAESKey =
                Base64.encodeToString(Crypto.encryptRSA(aesKeyBytes,recipientKey),
                        Base64.NO_WRAP);

        putJSONCommand(makeURL("send-message",recipient),
                keyValuePairs("aes-key", base64encryptedAESKey,
                        "sender",  base64AESEncrypted(sender, aesKey),
                        "recipient",  base64AESEncrypted(recipient, aesKey),
                        "subject-line",  base64AESEncrypted(subjectLine, aesKey),
                        "body",  base64AESEncrypted(body, aesKey),
                        "born-on-date",  base64AESEncrypted(bornOnDate.toString(), aesKey),
                        "time-to-live",  base64AESEncrypted(timeToLive.toString(), aesKey)),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(LOG, "MESSAGE SEND STATUS: " + response.toString());
                        handleSendResponse(messageReference, response);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        sendCommandFailed("Successfully Sent Encrypted Message!", volleyError);
                        volleyError.printStackTrace();
                        sendSendMessageFailed(messageReference, volleyError.networkResponse.statusCode + " ->  " + volleyError.getMessage());
                    }
                });

    }

    private void handleSendResponse(Object reference, JSONObject response){
        try {
            if (response.getString("status").equals("ok")) {
                Log.d(LOG, "Message sent succesful! OK");
                sendSendMessageSucceeded(reference);
            } else {
                Log.d(LOG, "MESSAGE SEND FAIL! " + response);
                Log.d(LOG, "Failed to send message. Reason: " + response.getString("reason"));
                sendSendMessageFailed(reference, response.getString("reason"));

            }
        } catch (JSONException e) {
            //sendSendMessageFailed(reference,"possible failure: unable to parse JSON response");
        }
    }

    private void getStringCommand(String url,
                                  Response.Listener<String> listener,
                                  Response.ErrorListener errorListener) {
        commandQueue.add(new StringRequest(Request.Method.GET, url, listener, errorListener));

    }

    private void getJSONCommand(String url,
                                Response.Listener<JSONObject> listener,
                                Response.ErrorListener errorListener){
        commandQueue.add(
                new JsonObjectRequest(Request.Method.GET, url, null, listener, errorListener));

    }

    private void putJSONCommand(String url, JSONObject json,
                                Response.Listener<JSONObject> listener,
                                Response.ErrorListener errorListener){
        commandQueue.add(
                new JsonObjectRequest(Request.Method.PUT, url, json, listener, errorListener));

    }

    private JSONObject keyValuePairs(String... args){
        JSONObject json = new JSONObject();
        try {
            for(int i=0; i+1<args.length;i+=2){
                json.put(args[i],args[i+1]);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public class UserInfo{
        public final String username;
        public final String image;
        public final String publicKey;
        public UserInfo(String username, String image, String keyString){
            this.username = username;
            this.image = image;
            this.publicKey = keyString; //  Crypto.getPublicKeyFromString()
        }
    }

    public interface Listener {
        void onLastRecievedMessages(ArrayList<EmailData> emails);
        void onCommandFailed(String commandName, VolleyError volleyError);
        void onGoodAPIVersion();
        void onBadAPIVersion();
        void onRegistrationSucceeded();
        void onRegistrationFailed(String reason);
        void onLoginSucceeded(String username);
        void onLoginFailed(String reason);
        void onLogoutSucceeded();
        void onLogoutFailed(String reason);
        void onUserInfo(UserInfo info);
        void onUserNotFound(String username);
        void onContactLogin(String username);
        void onContactLogout(String username);
        void onSendMessageSucceeded(Object key);
        void onSendMessageFailed(Object key, String reason);
        void onMessageDelivered(String sender, String recipient,
                                String subject, String body,
                                long born_on_date,
                                long time_to_live);

        void onRegisterContactsResponse(HashMap<String, String> friends_status);
    }

    private ArrayList<Listener> myListeners = new ArrayList<>();

    public void registerListener(Listener listener){myListeners.add(listener);}
    public void unregisterListener(Listener listener){myListeners.remove(listener);}

    private void sendCommandFailed(String commandName, VolleyError volleyError){
        for(Listener listener : myListeners){
            listener.onCommandFailed(commandName,volleyError);
        }
    }

    private void sendGoodAPIVersion(){
        for(Listener listener : myListeners){
            listener.onGoodAPIVersion();
        }
    }

    private void sendLastRecievedMessages(ArrayList<EmailData> emails) {
        for(Listener listener : myListeners) {
            listener.onLastRecievedMessages(emails);
        }
    }


    private void sendBadAPIVersion(){
        for(Listener listener : myListeners){
            listener.onBadAPIVersion();
        }
    }

    private void sendRegistrationSucceeded(){
        for(Listener listener : myListeners){
            listener.onRegistrationSucceeded();
        }
    }
    private void sendRegistrationFailed(String reason){
        for(Listener listener : myListeners){
            listener.onRegistrationFailed(reason);
        }
    }

    private void sendLoginSucceeded(String username){
        for(Listener listener : myListeners){
            listener.onLoginSucceeded(username);
        }
    }

    private void sendLoginFailed(String reason){
        for(Listener listener : myListeners){
            listener.onLoginFailed(reason);
        }
    }

    private void sendLogoutSucceeded(){
        for(Listener listener : myListeners){
            listener.onLogoutSucceeded();
        }
    }

    private void sendLogoutFailed(String reason){
        for(Listener listener : myListeners){
            listener.onLogoutFailed(reason);
        }
    }
    private void sendUserInfo(UserInfo info){
        for(Listener listener : myListeners){
            listener.onUserInfo(info);
        }
    }
    private void sendUserNotFound(String username){
        for(Listener listener : myListeners){
            listener.onUserNotFound(username);
        }
    }
    private void sendContactLogin(String username){
        for(Listener listener : myListeners){
            listener.onContactLogin(username);
        }
    }
    private void sendContactLogout(String username){
        for(Listener listener : myListeners){
            listener.onContactLogout(username);
        }
    }

    private void sendSendMessageSucceeded(Object key){
        for(Listener listener: myListeners){
            listener.onSendMessageSucceeded(key);
        }
    }

    public void sendRegisterContactsResponse(HashMap<String,String> friends_status) {
        for(Listener listener: myListeners){
            listener.onRegisterContactsResponse(friends_status);
        }
    }

    private void sendSendMessageFailed(Object key, String reason){
        for(Listener listener: myListeners){
            listener.onSendMessageFailed(key, reason);
        }
    }

    private void sendMessageDelivered(String sender,String recipient,
                                      String subject,String body,
                                      long born_on_date,
                                      long time_to_live){
        for(Listener listener : myListeners) {
            listener.onMessageDelivered(
                    sender, recipient,
                    subject, body,
                    born_on_date, time_to_live);
        }
    }


    public void registerListener() {
        registerListener(new ServerAPI.Listener() {
            @Override
            public void onLastRecievedMessages(ArrayList<EmailData> emails) {
                for (EmailData rcvEmail : emails) {
                    Log.d(LOG, rcvEmail.toString());
                }
            }

            @Override
            public void onCommandFailed(String commandName, VolleyError volleyError) {
                Toast.makeText(appContext, String.format("command %s failed!", commandName),
                        Toast.LENGTH_SHORT).show();
                volleyError.printStackTrace();
            }

            @Override
            public void onGoodAPIVersion() {
                Toast.makeText(appContext, "API Version Matched!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBadAPIVersion() {
                Toast.makeText(appContext, "API Version Mismatch!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRegistrationSucceeded() {
                Toast.makeText(appContext, "Registered!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRegistrationFailed(String reason) {
                Toast.makeText(appContext, "Not registered!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoginSucceeded(String username) {
                Toast.makeText(appContext, "Logged in!", Toast.LENGTH_SHORT).show();

                SharedPreferences settings = appContext.getSharedPreferences(AppConstants.MY_PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("username", username);
                editor.commit();
            }

            @Override
            public void onLoginFailed(String reason) {
                Toast.makeText(appContext, "Not logged in!" + "reason: " + reason, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLogoutSucceeded() {
                Toast.makeText(appContext, "Logged out!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLogoutFailed(String reason) {
                Toast.makeText(appContext, "Not logged out!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUserInfo(ServerAPI.UserInfo info) {
                Log.d(LOG, "Got userinfo: name: " + info.username + " pk: " + info.publicKey);
            }

            @Override
            public void onUserNotFound(String username) {
                Toast.makeText(appContext, String.format("user %s not found!", username), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onContactLogin(String username) {
                Toast.makeText(appContext, String.format("user %s logged in", username), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onContactLogout(String username) {
                Toast.makeText(appContext, String.format("user %s logged out", username), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSendMessageSucceeded(Object key) {
                Toast.makeText(appContext, String.format("Message Sent Successfully!"), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSendMessageFailed(Object key, String reason) {
                Toast.makeText(appContext, String.format("Failed to send the message.\nReason: " + reason), Toast.LENGTH_SHORT).show();
                Log.d(LOG, "FAILED to send message !!!: " + reason);
            }

            @Override
            public void onMessageDelivered(String sender, String recipient, String subject, String body, long born_on_date, long time_to_live) {
                Toast.makeText(appContext, String.format("\nT\nGOT message from %s", sender), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onRegisterContactsResponse(HashMap<String, String> friends_status) {
                //Toast.makeText(appContext, String.format("Registered contacts: %s", friends_status.toString()), Toast.LENGTH_SHORT).show();
                //Log.d(LOG, "Registered contacts: " + friends_status.toString());
            }
        });

    }


}
