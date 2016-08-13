package myhomework.com.meilmanager.model;

/**
 * Created by Misho on 6/16/2016.
 */
public class EmailData {
    String _EmailID;
    String _Sender;
    String _Subject;
    String _Body;
    String _Time;

    public EmailData(){

    }
    // constructor
    public EmailData(String EmailID, String Sender, String Subject, String Body, String sTime){
        this._EmailID = EmailID;
        this._Sender = Sender;
        this._Subject = Subject;
        this._Body = Body;
        this._Time = sTime;
    }
    public String getEmailID() {
        return  this._EmailID;
    }
    public void setEmailID(String userID) {
        this._EmailID = userID;
    }
    public String getSender() {
        return this._Sender;
    }
    public void setSender(String Sender){
        this._Sender = Sender;
    }
    public String getSubject() {
        return this._Subject;
    }
    public void setSubject(String Subject){
        this._Subject = Subject;
    }
    public String getBody() {
        return this._Body;
    }
    public void setBody(String Body){
        this._Body = Body;
    }
    public String getTime() {
        return _Time;
    }
    public void setTime(String strTime) {
        this._Time = strTime;
    }

    @Override
    public String toString() {
        return "sender: " + _Sender
                + "subject: " + _Subject
                + "body: " + _Body
                + "ttl" + _Time;

    }
}
