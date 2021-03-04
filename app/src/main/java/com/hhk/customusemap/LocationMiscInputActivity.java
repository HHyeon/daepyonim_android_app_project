package com.hhk.customusemap;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class LocationMiscInputActivity extends AppCompatActivity {

    static final String INTENT_DATA_FROM_INPUTACTIVITY_ID = "INTENT_DATA_FROM_INPUTACTIVITY_ID";
    static final String INTENT_DATA_FROM_INPUTACTIVITY_LAT = "INTENT_DATA_FROM_INPUTACTIVITY_LAT";
    static final String INTENT_DATA_FROM_INPUTACTIVITY_LON = "INTENT_DATA_FROM_INPUTACTIVITY_LON";
    static final String INTENT_DATA_FROM_INPUTACTIVITY_TITLE = "INTENT_DATA_FROM_INPUTACTIVITY_TITLE";

    private static final int REQUEST_GALLERY = 0x2000;

    private ArrayList<MiscInfoListItem> miscInfoListItems = new ArrayList<MiscInfoListItem>();
    private MiscInfoListAdapter miscInfoListAdapter;
    private EditText edit_titleinput;
    private String currentlocationmapaddressname;
    private double latitude, longitude;
    private String ThisPhoneLineNumber = null;
    private SimpleLoadingProgressbarDialog progressbarDialog;
    private Uri UriimagePicked = null;
    //    private ImageButton imgbutton_imagepick;
    ImageUploadSkipYesOrNoDialog simpleYesNoDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_misc_input);

//        imgbutton_imagepick = findViewById(R.id.imgbutton_imagepick);
        edit_titleinput = findViewById(R.id.edit_titleinput);
        TextView location_address_name_preview = findViewById(R.id.location_address_name_preview);
        ListView listview_misc_info_input_edittext = findViewById(R.id.listview_misc_info_input_edittext);
        miscInfoListAdapter = new MiscInfoListAdapter();
        listview_misc_info_input_edittext.setAdapter(miscInfoListAdapter);

        currentlocationmapaddressname = getIntent().getStringExtra(MainActivity.ACTIVITY_TRANSFER_KEY_ADDRNAME);
        latitude = getIntent().getDoubleExtra(MainActivity.GEO_COORD_LAT_TRANSFER_KEY, -1);
        longitude = getIntent().getDoubleExtra(MainActivity.GEO_COORD_LON_TRANSFER_KEY,-1);
        ThisPhoneLineNumber = getIntent().getStringExtra(MainActivity.INTENT_DATA_PHONENUMBER);
        location_address_name_preview.setText(currentlocationmapaddressname);
    }


    public void ItemAddRemoveBtn(View v) {
        int id = v.getId();
        if(id == R.id.imgbutton_removeitem) {
            miscInfoListAdapter.removeItem();
            miscInfoListAdapter.notifyDataSetChanged();
        }
        else if(id == R.id.imgbutton_additem) {
            miscInfoListAdapter.addItem();
            miscInfoListAdapter.notifyDataSetChanged();
        }
        else if(id == R.id.imgbutton_ApplyAll) {
            if(!edit_titleinput.getEditableText().toString().isEmpty()){

                for(int i=0;i<miscInfoListItems.size();i++) {
                    String name = miscInfoListItems.get(i).getName();
                    String phnum = miscInfoListItems.get(i).getPhoneNumber();
                    if(name.isEmpty() && phnum.isEmpty()) {
                        Toast.makeText(this, getString(R.string.somethingempty), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                if(UriimagePicked == null) {
                    simpleYesNoDialog = new ImageUploadSkipYesOrNoDialog(getString(R.string.imageskip_yndialog_title),
                            LocationMiscInputActivity.this, new ImageUploadSkipYesOrNoDialog.ButtonsClickListener() {
                        @Override
                        public void onYesBtnClick() {
                            DBserverworker dBserverworker = new DBserverworker();
                            dBserverworker.start();
                        }

                        @Override
                        public void onNoBtnClick() {
                        }
                    });
                    simpleYesNoDialog.show();
                }
                else {
                    DBserverworker dBserverworker = new DBserverworker();
                    dBserverworker.start();
                }

            }
            else {
                Toast.makeText(this, getString(R.string.EmptyEditInput), Toast.LENGTH_SHORT).show();
            }
        }
        else if(id == R.id.imgbutton_imagepick) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_GALLERY);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_GALLERY) {
            if (resultCode == Activity.RESULT_OK) {
                assert data != null;
                final Uri uri = data.getData();
                assert uri != null;

                UriimagePicked = uri;
            }
        }
    }

    private static class MiscInfoListItem {
        private String Name;
        private String PhoneNumber;

        MiscInfoListItem(String Name, String PhoneNumber) {
            this.Name = Name;
            this.PhoneNumber = PhoneNumber;
        }

        String getName() {
            return Name;
        }

        String getPhoneNumber() {
            return PhoneNumber;
        }

        void setName(String s) {
            Name = s;
        }

        void setPhoneNumber(String s) {
            PhoneNumber = s;
        }
    }

    private class MiscInfoListAdapter extends BaseAdapter {


        @Override
        public int getCount() {
            return miscInfoListItems.size();
        }

        @Override
        public Object getItem(int position) {
            return miscInfoListItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final Context context = parent.getContext();
            final MisclayoutViewHolder holder;

            if(convertView == null) {
                holder = new MisclayoutViewHolder();
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.misc_info_input_item_layout,parent,false);
                holder.editText1 = convertView.findViewById(R.id.edit_misc_info_name_input);
                holder.editText2 = convertView.findViewById(R.id.edit_misc_info_phonenumber_input);
                convertView.setTag(holder);
            }
            else {
                holder = (MisclayoutViewHolder) convertView.getTag();
            }
            holder.ref = position;

            final MiscInfoListItem item = miscInfoListItems.get(position);

            holder.editText1.setText(item.getName());
            holder.editText2.setText(item.getPhoneNumber());

            holder.editText1.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    miscInfoListItems.get(holder.ref).setName(s.toString());
                }
            });

            holder.editText2.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    miscInfoListItems.get(holder.ref).setPhoneNumber(s.toString());
                }
            });

            return convertView;
        }

        void addItem() {
            MiscInfoListItem item = new MiscInfoListItem("", "");
            miscInfoListItems.add(item);
        }

        void removeItem() {
            if(miscInfoListItems.size()>0) {
                int len = miscInfoListItems.size() - 1;
                miscInfoListItems.remove(len);
            }
        }
    }

    static private class MisclayoutViewHolder {
        EditText editText1;
        EditText editText2;
        int ref;
    }

    private class DBserverworker extends Thread {

        private String title;

        DBserverworker() {
            title = edit_titleinput.getEditableText().toString();
            progressbarDialog = new SimpleLoadingProgressbarDialog(LocationMiscInputActivity.this);
            progressbarDialog.show();
        }

        private String majordatainsert() throws Exception {
            HttpURLConnection connection;
            String httprequeststring = getString(R.string.Server_Inet_ADDR)+"?method=2" +
                    "&a[]=" + title +
                    "&a[]=" + currentlocationmapaddressname +
                    "&a[]=" + ThisPhoneLineNumber +
                    "&a[]=" + latitude +
                    "&a[]=" + longitude;

            Log.d(MainActivity.TAG, httprequeststring);

            URL url = new URL(httprequeststring);
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

        private int insert_profile_partitionly(String urlstr) throws Exception {
            int uploaded;
            HttpURLConnection connection;
            Log.d(MainActivity.TAG, "UPLOAD !! =>" + urlstr);
            URL url = new URL(urlstr);
            connection = (HttpURLConnection)url.openConnection();
            BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String str;
            StringBuilder buffer = new StringBuilder();
            while((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
            }
            bufferedReader.close();
            connection.disconnect();

            JSONObject jsonObjectSrc;
            jsonObjectSrc = new JSONObject(buffer.toString());
            String resstr = jsonObjectSrc.getString("work");

            if(resstr.equals("ok")) {
                JSONArray resultdata = jsonObjectSrc.getJSONArray("result");
                JSONObject resulteach = resultdata.getJSONObject(0);
                uploaded = resulteach.getInt("uploded");
            }
            else {
                uploaded = -1;
            }

            return uploaded;
        }

        private int profile_insert_all(int id) throws Exception {
            StringBuilder urlstr;
            int divideupload = 0;
            int n = 0;

            urlstr = new StringBuilder(getString(R.string.Server_Inet_ADDR) + "?method=4&id="+id);
            for(int i=0;i<miscInfoListItems.size();i++) {
                String name = miscInfoListItems.get(i).getName();
                String phnum = miscInfoListItems.get(i).getPhoneNumber();

                urlstr.append("&a[]=").append(name).append("&b[]=").append(phnum);

                divideupload++;
                if(divideupload % 10 == 0) {
                    int nn = insert_profile_partitionly(urlstr.toString());
                    if(nn != -1) {
                        n += nn;
                    }
                    else {
                        Log.d(MainActivity.TAG, "ERROR");
                        return -1;
                    }

                    divideupload = 0;
                    urlstr = new StringBuilder(getString(R.string.Server_Inet_ADDR) + "?method=4&id="+id);
                }
            }

            if(divideupload != 0) {
                int nn = insert_profile_partitionly(urlstr.toString());
                if(nn != -1) {
                    n += nn;
                }
                else {
                    Log.d(MainActivity.TAG, "ERROR");
                    return -1;
                }
            }

            return n;
        }

        private void delete_current_item_wrong_session(int itemid) throws Exception {
            HttpURLConnection connection;
            URL url = new URL(getString(R.string.Server_Inet_ADDR)+"?method=3" + "&index=" + itemid);
            connection = (HttpURLConnection)url.openConnection();
            BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String str;
            StringBuilder buffer = new StringBuilder();
            while((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
            }
            bufferedReader.close();
            connection.disconnect();
            // TODO or not Json Parse and delete status check
        }

        @Override
        public void run() {
            boolean success;
            int itemid = -1;
            try {
                String jsonraw = majordatainsert();
                JSONObject jsonObjectSrc;
                jsonObjectSrc = new JSONObject(jsonraw);
                String resstr = jsonObjectSrc.getString("work");
                success = resstr.equals("insert");

                Log.d(MainActivity.TAG, "header added  :" + jsonraw);

                if(success) {
                    JSONArray resultdata = jsonObjectSrc.getJSONArray("result");
                    JSONObject resulteach = resultdata.getJSONObject(0);
                    itemid = resulteach.getInt("id");

                    Log.d(MainActivity.TAG, "peoplesprofile added  :" + resultdata);

                    int uploaded = profile_insert_all(itemid);
                    if(uploaded != miscInfoListItems.size()) {
                        success = false;
                    }

                    if(UriimagePicked != null) {
                        String uritype = getContentResolver().getType(UriimagePicked);
                        assert uritype != null;
                        String remotefilename = itemid+"."+ uritype.split("/")[1];

                        FTPClient ftpClient = new FTPClient();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(UriimagePicked);
                            assert inputStream != null;

                            ftpClient.connect(getString(R.string.FtpServerAddress), 16021);
                            ftpClient.login("hyeon", "001123");
                            ftpClient.changeWorkingDirectory("uplodedimages");
                            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                            ftpClient.enterRemotePassiveMode();
                            ftpClient.enterLocalPassiveMode();

                            Log.d(MainActivity.TAG, "ftp uploading...");
                            if(ftpClient.storeFile(remotefilename , inputStream)) {
                                Log.i(MainActivity.TAG, "ftp store file successed");
                            }
                            else {
                                Log.e(MainActivity.TAG, "ftp store file FAILED!!!!!!");
                            }
                            inputStream.close();

                            ftpClient.noop();

                        } catch (IOException e) {
                            Log.e(MainActivity.TAG, e.getMessage());
                            e.printStackTrace();
                        }
                        finally {
                            try {
                                ftpClient.disconnect();
                            } catch (IOException ignored) {
                            }
                        }

                    }

                    Log.d(MainActivity.TAG, "uploaded : " + uploaded);
                }
            } catch (Exception e) {
                Log.e(MainActivity.TAG, e.getMessage());
                e.printStackTrace();
                success = false;
            }

            try {
                if(!success) {
                    if(itemid != -1) {
                        delete_current_item_wrong_session(itemid);
                    }
                }
            } catch (Exception e) {
                Log.e(MainActivity.TAG, e.getMessage());
            }

            final boolean finalsucc = success;
            final int finalItemid = itemid;
            LocationMiscInputActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(finalsucc) {
                        Toast.makeText(LocationMiscInputActivity.this, getString(R.string.serverinsertsuccessfull), Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent();
                        intent.putExtra(INTENT_DATA_FROM_INPUTACTIVITY_ID, finalItemid);
                        intent.putExtra(INTENT_DATA_FROM_INPUTACTIVITY_TITLE, title);
                        intent.putExtra(INTENT_DATA_FROM_INPUTACTIVITY_LAT, latitude);
                        intent.putExtra(INTENT_DATA_FROM_INPUTACTIVITY_LON, longitude);

                        setResult(Activity.RESULT_OK, intent);
                        finish();
                    }
                    else
                        Toast.makeText(LocationMiscInputActivity.this, getString(R.string.ERRORS), Toast.LENGTH_SHORT).show();
                }
            });

            progressbarDialog.dismiss();

        }
    }



/*
    ImageUploadWorker imageUploadWorker;
    class ImageUploadWorker extends Thread {
        SimpleLoadingProgressbarDialog dialog;
        private Uri uri;
        private String remotefilename;

        ImageUploadWorker(Uri uri, String remotefilename) {
            dialog = new SimpleLoadingProgressbarDialog(LocationMiscInputActivity.this);
            String uritype = getContentResolver().getType(uri);
            assert uritype != null;

            this.uri = uri;
            this.remotefilename = remotefilename+"."+ uritype.split("/")[1];
        }

        @Override
        public void run() {
            FTPClient ftpClient = new FTPClient();
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                assert inputStream != null;

                ftpClient.connect("192.168.0.15", 21);
                ftpClient.login("hyeon", "001123");
                ftpClient.changeWorkingDirectory("uplodedimages");

                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                if(ftpClient.storeFile(remotefilename , inputStream)) {
                    Log.i(MainActivity.TAG, "ftp store file successed");
                }
                else {
                    Log.e(MainActivity.TAG, "ftp store file FAILED!!!!!!");
                }
                inputStream.close();
                ftpClient.noop();
                ftpClient.logout();
            } catch (IOException e) {
                Log.e(MainActivity.TAG, e.getMessage());
                e.printStackTrace();
            }
            finally {
                try {
                    ftpClient.disconnect();
                } catch (IOException ignored) {
                }
            }

            dialog.dismiss();
        }
    }
    */
}
