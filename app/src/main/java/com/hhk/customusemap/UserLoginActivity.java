package com.hhk.customusemap;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UserLoginActivity extends AppCompatActivity {

    static final String INTENT_DATA_FROM_USER_LOGIN = "INTENT_DATA_FROM_USER_LOGIN";
    EditText edit_userlogin_idinput;
    String ThisPhoneLineNumber;
    String UserIdentifyName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);
        edit_userlogin_idinput = findViewById(R.id.edit_userlogin_idinput);
        ThisPhoneLineNumber = getIntent().getStringExtra(MainActivity.INTENT_DATA_PHONENUMBER);
    }

    public void onBtnClickLoginActivity(View v) {
        int id = v.getId();
        if(id == R.id.btn_useridinputconfirm) {
            String input = edit_userlogin_idinput.getEditableText().toString();
            if(input.length() > 0) {
                dBserverworker = new DBserverworker(input);
                dBserverworker.start();
            }
            else {
                Toast.makeText(this, getString(R.string.somethingempty), Toast.LENGTH_SHORT).show();
            }
        }
    }

    DBserverworker dBserverworker;

    private class DBserverworker extends Thread {
        String name;

        DBserverworker(String name) {
            this.name = name;
        }

        String registeruser() throws IOException {
            HttpURLConnection connection;
            URL url = new URL(getString(R.string.Server_Inet_ADDR)+"?method=6" +
                    "&assignedName=" + name +
                    "&PhoneNumber=" + ThisPhoneLineNumber );
            connection = (HttpURLConnection)url.openConnection();
            BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String str;
            StringBuilder buffer = new StringBuilder();
            while((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
            }
            bufferedReader.close();
            connection.disconnect();
            return buffer.toString();
        }

        @Override
        public void run() {

            try {
                String resultsrc = registeruser();

                JSONObject jsonObjectSrc;
                jsonObjectSrc = new JSONObject(resultsrc);
                String resstr = jsonObjectSrc.getString("work");

                if(resstr.equals("ok")) {

                    Intent intent = new Intent();
                    intent.putExtra(INTENT_DATA_FROM_USER_LOGIN, name);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }
}
