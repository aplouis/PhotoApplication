package com.fogoa.photoapplication.data.models;

import android.util.Log;

import com.fogoa.photoapplication.misc.Constants;
import com.fogoa.photoapplication.misc.Utilities;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Date;

public class UserItem {
    private static final String TAG = UserItem.class.getSimpleName();

    public String userid = "";
    public String email_address = "";
    public String username = "";
    public String password = "";
    public String first_name = "";
    public String last_name = "";
    public String phone = "";
    public String locale = "";
    public Date member_since;

    public UserItem() { }

    public UserItem(String jsonString) {
        ParseJson(jsonString);
    }

    public UserItem(JSONObject jsonObj) {
        ParseJson(jsonObj);
    }

    public void ParseJson(String jsonString) {
        if (jsonString != null && jsonString.length() > 0) {
            try {
                Object objResp = new JSONTokener(jsonString).nextValue();
                if (objResp instanceof JSONObject) {
                    JSONObject jsonObj = (JSONObject)objResp;
                    ParseJson(jsonObj);
                }
            }
            catch (JSONException e) {
                //json is invlid
                if (Constants.DEBUG) Log.e(TAG, "json parse exception : " + e.toString());
            }

        }

    }

    public void ParseJson(JSONObject jsonObj) {
        try {
            if (jsonObj.has("user_id")) {
                userid = jsonObj.getString("user_id");
            }
            if (jsonObj.has("email_address")) {
                email_address = jsonObj.getString("email_address");
            }
            if (jsonObj.has("username")) {
                username = jsonObj.getString("username");
            }
            if (jsonObj.has("password")) {
                password = jsonObj.getString("password");
            }
            if (jsonObj.has("first_name")) {
                first_name = jsonObj.getString("first_name");
            }
            if (jsonObj.has("last_name")) {
                last_name = jsonObj.getString("last_name");
            }
            if (jsonObj.has("phone")) {
                phone = jsonObj.getString("phone");
            }
            if (jsonObj.has("locale")) {
                locale = jsonObj.getString("locale");
            }
            if (jsonObj.has("member_since_utc")) {
                member_since = Utilities.safeParseDate(jsonObj.getString("member_since_utc"));
            }
        }
        catch (JSONException e) {
            //json is invlid
            if (Constants.DEBUG) Log.e(TAG, "json parse exception : " + e.toString());
        }

    }

    public String GetJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("user_id", userid);
            json.put("email_address", email_address);
            json.put("username", username);
            json.put("password", password);
            json.put("first_name", first_name);
            json.put("last_name", last_name);
            json.put("phone", phone);
            json.put("locale", locale);
            json.put("member_since_utc", Utilities.safeDateToString(member_since));

            return json.toString();
        }
        catch (JSONException e) {
            String strJson = "{" +
                    "\"user_id\":\""+userid+"\"" +
                    ",\"email_address\":\""+email_address+"\"" +
                    ",\"username\":\""+username+"\"" +
                    ",\"password\":"+password+"" +
                    ",\"first_name\":\""+first_name+"\"" +
                    ",\"last_name\":\"" + last_name + "\"" +
                    ",\"phone\":\"" + phone + "\"" +
                    ",\"locale\":\"" + locale + "\"" +
                    ",\"member_since_utc\":\"" + Utilities.safeDateToString(member_since) + "\"";
            strJson += "}";
            return strJson;
        }
    }

    public String toString() {
        return GetJson();
    }

}
