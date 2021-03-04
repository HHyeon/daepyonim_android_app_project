package com.hhk.customusemap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DetailedInfoPanel extends AppCompatActivity {

    private String title = null, dt, who, map;
    private TextView tv_detailedinfo_title, tv_detailedinfo_dt, tv_detailedinfo_who, tv_detailedinfo_map, tv_nodata;
    private recyclerview_adapter listadapter;
    private Uri localimageuri = null;
    private SimpleLoadingProgressbarDialog progressbarDialog;
    private ImageView imageview_detailedpanel;
    ImageUploadSkipYesOrNoDialog simpleYesNoDialog;

    int itemid;
    String UserIdentifyName = null;

    MenuItem menuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detailed_info_panel_layout);

        Toolbar toolbar = findViewById(R.id.actionbar_detailedpanel);
        setSupportActionBar(toolbar);
        setTitle(R.string.infodetailed);

        tv_detailedinfo_title = findViewById(R.id.tv_detailedinfo_title);
        tv_detailedinfo_dt = findViewById(R.id.tv_detailedinfo_dt);
        tv_detailedinfo_who = findViewById(R.id.tv_detailedinfo_who);
        tv_detailedinfo_map = findViewById(R.id.tv_detailedinfo_map);
        tv_nodata = findViewById(R.id.tv_nodata);
        RecyclerView recyclerview_detailedinfo = findViewById(R.id.recyclerview_detailedinfo);
        imageview_detailedpanel = findViewById(R.id.imageview_detailedpanel);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerview_detailedinfo.setHasFixedSize(true);
        recyclerview_detailedinfo.setLayoutManager(linearLayoutManager);

        itemid = getIntent().getIntExtra(MainActivity.INTENT_DATA_ITEM_ID, -1);
        if(itemid == -1) {
            finish();
        }
        UserIdentifyName = getIntent().getStringExtra(MainActivity.INTENT_DATA_IDENTIFYNAME);

        if(UserIdentifyName == null)  {
            Toast.makeText(this, "UserIdentifyName is null", Toast.LENGTH_SHORT).show();
            finish();
        }

        listadapter = new recyclerview_adapter();
        recyclerview_detailedinfo.setAdapter(listadapter);

        DBserverworker dBserverworker = new DBserverworker();
        dBserverworker.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detailed_panel, menu);
        menuItem = menu.findItem(R.id.btnmenu_delete_this_item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.btnmenu_delete_this_item) {
            simpleYesNoDialog = new ImageUploadSkipYesOrNoDialog(getString(R.string.deleteconfirm),
                    this, new ImageUploadSkipYesOrNoDialog.ButtonsClickListener() {
                @Override
                public void onYesBtnClick() {
                    DBserverworker_delete worker = new DBserverworker_delete();
                    worker.start();
                }

                @Override
                public void onNoBtnClick() {
                }
            });
            simpleYesNoDialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    static class recyclerview_item {
        private String name;
        private String phnum;

        recyclerview_item(String name, String phnum) {
            this.name = name;
            this.phnum = phnum;
        }

        String getName() {
            return name;
        }

        String getPhnum() {
            return phnum;
        }
    }

    static class recyclerview_adapter extends RecyclerView.Adapter<recyclerview_adapter.CustomViewHolder> {

        private List<recyclerview_item> listItems = new ArrayList<recyclerview_item>();

        void AddItem(String nm, String ph) {
            listItems.add(new recyclerview_item(nm, ph));
        }

        @NonNull
        @Override
        public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.detailed_panel_people_info_item, parent, false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
            recyclerview_item item = listItems.get(position);
            holder.tv_detailedinfo_item_name.setText(item.getName());
            holder.tv_detailedinfo_item_phnum.setText(item.getPhnum());
        }

        @Override
        public int getItemCount() {
            return listItems.size();
        }

        static class CustomViewHolder extends  RecyclerView.ViewHolder {
            TextView tv_detailedinfo_item_name;
            TextView tv_detailedinfo_item_phnum;
            CustomViewHolder(View v) {
                super(v);
                tv_detailedinfo_item_name = v.findViewById(R.id.tv_detailedinfo_item_name);
                tv_detailedinfo_item_phnum = v.findViewById(R.id.tv_detailedinfo_item_phnum);
            }
        }
    }


    private class DBserverworker_delete extends Thread {

        DBserverworker_delete() {
            progressbarDialog = new SimpleLoadingProgressbarDialog(DetailedInfoPanel.this);
            progressbarDialog.show();
        }

        private String retrievedata() throws Exception { // 1 or 2 only method value
            HttpURLConnection connection;
            URL url = new URL(getString(R.string.Server_Inet_ADDR)+"?method=7&id="+itemid);
            connection = (HttpURLConnection) url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String str;
            StringBuilder buffer = new StringBuilder();
            while ((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
            }
            bufferedReader.close();
            connection.disconnect();
            return buffer.toString();
        }

        @Override
        public void run() {
            try {
                String str = retrievedata();
                Log.d(MainActivity.TAG, str);


                FTPClient ftpClient = new FTPClient();
                ftpClient.connect(getString(R.string.FtpServerAddress), 16021);
                ftpClient.login("hyeon", "001123");
                ftpClient.changeWorkingDirectory("uplodedimages");
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                ftpClient.enterRemotePassiveMode();
                ftpClient.enterLocalPassiveMode();

                String remotefilename = null;
                String[] resultlist = ftpClient.listNames(itemid + ".*");
                for(String s : resultlist) {
                    Log.d(MainActivity.TAG, ": " + s);
                    remotefilename = s;
                    break;
                }

                if(remotefilename == null) {
                    Log.d(MainActivity.TAG, "file " + itemid + " item is not exists");
                }
                else {
                    Log.d(MainActivity.TAG, "delete file " + itemid + " item image");

                    if(ftpClient.deleteFile(remotefilename)){
                        Log.d(MainActivity.TAG,"file deleted from server");
                    }
                    else {
                        Log.d(MainActivity.TAG,"file NOT deleted from server");
                    }
                }
                ftpClient.disconnect();


            } catch (Exception e) {
                e.printStackTrace();
            }
            progressbarDialog.dismiss();

            Intent intent = new Intent();
            intent.putExtra(INTENT_DATA_FROM_POI_DELETE_REQUEST, itemid);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    final static String INTENT_DATA_FROM_POI_DELETE_REQUEST = "INTENT_DATA_FROM_POI_DELETE_REQUEST";

    private class DBserverworker extends Thread {

        DBserverworker() {
            progressbarDialog = new SimpleLoadingProgressbarDialog(DetailedInfoPanel.this);
            progressbarDialog.show();
        }

        private String retrievedata(int method) throws Exception { // 1 or 2 only method value
            HttpURLConnection connection;
            URL url = new URL(getString(R.string.Server_Inet_ADDR) + "?method="+method+ "&id="+itemid);
            connection = (HttpURLConnection) url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String str;
            StringBuilder buffer = new StringBuilder();
            while ((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
            }
            bufferedReader.close();
            connection.disconnect();
            return buffer.toString();
        }

        @Override
        public void run() {
            JSONObject jsonObjectSrc;

            try {
                String jsonraw = retrievedata(1); // header data
                jsonObjectSrc = new JSONObject(jsonraw);
                String resstr = jsonObjectSrc.getString("work");
                if(resstr.equals("empty")) {
                    DetailedInfoPanel.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressbarDialog.dismiss();
                            Toast.makeText(DetailedInfoPanel.this, "Deleted Item", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }

                if(resstr.equals("select")) {
                    JSONArray resultdata = jsonObjectSrc.getJSONArray("result");
                    if(resultdata.length() == 1) {
                        JSONObject resulteach = resultdata.getJSONObject(0);
                        title = resulteach.getString("title");
                        dt = resulteach.getString("dt");
                        map = resulteach.getString("map");
                        who = resulteach.getString("who");

                        Log.d(MainActivity.TAG, "title : " + title + ", dt : " + dt + ", map : " + map + ", who : " +who);
                    }
                }
                else {
                    DetailedInfoPanel.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressbarDialog.dismiss();
                            Toast.makeText(DetailedInfoPanel.this, "Deleted Item", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                    return;
                }

                jsonraw = retrievedata(5); // peoples data
                jsonObjectSrc = new JSONObject(jsonraw);
                resstr = jsonObjectSrc.getString("work");

                final boolean showemptyinfo;

                if(resstr.equals("empty")) {
                    Log.d(MainActivity.TAG, "peoplesdata empty");
                    showemptyinfo = true;
                }
                else {
                    showemptyinfo = false;
                    int cnts = Integer.parseInt(resstr);
                    if(cnts > 0){
                        JSONArray resultdata = jsonObjectSrc.getJSONArray("result");
                        for(int i=0;i<resultdata.length();i++) {
                            JSONObject resulteach = resultdata.getJSONObject(i);
                            String nm = resulteach.getString("nm");
                            String ph = resulteach.getString("ph");
                            listadapter.AddItem(nm, ph);
                        }
                    }
                }

                FTPClient ftpClient = new FTPClient();
                ftpClient.connect(getString(R.string.FtpServerAddress), 16021);
                ftpClient.login("hyeon", "001123");
                ftpClient.changeWorkingDirectory("uplodedimages");
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                ftpClient.enterRemotePassiveMode();
                ftpClient.enterLocalPassiveMode();

                File workdir = getCacheDir();
                String[] nameslist = workdir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.contains(itemid + ".");
                    }
                });
                // check image in cacheDir before download
                if(nameslist.length > 0) {
                    String filename = nameslist[0];
                    localimageuri = Uri.fromFile(new File(getCacheDir(), filename));
                    Log.d(MainActivity.TAG, "file " + filename + " is in cache");
                }
                else {
                    Log.d(MainActivity.TAG, "downloading file which id is " + itemid);

                    String remotefilename = null;
                    String[] resultlist = ftpClient.listNames(itemid + ".*");

                    if(resultlist != null) {
                        if (resultlist.length > 0) {
                            remotefilename = resultlist[0];
                            Log.d(MainActivity.TAG, "found in server. filename : " + remotefilename);
                        }
                    }

                    if(remotefilename != null) {
                        Log.d(MainActivity.TAG, "found in server " + remotefilename );
                        File file = new File(getCacheDir(), remotefilename);

                        FileOutputStream fileOutputStream = new FileOutputStream(file);

                        if(ftpClient.retrieveFile(remotefilename, fileOutputStream)) {
                            localimageuri = Uri.fromFile(new File(getCacheDir(), remotefilename));
                            Log.i(MainActivity.TAG, "file received : " + remotefilename);
                        }
                        else {
                            Log.e(MainActivity.TAG, "file received ERROR");
                        }
                    }
                    else {
                        Log.d(MainActivity.TAG, "image " + itemid + " is not found in server");
                    }
                }

                ftpClient.disconnect();

                DetailedInfoPanel.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(showemptyinfo) tv_nodata.setVisibility(View.VISIBLE);
                        else tv_nodata.setVisibility(View.INVISIBLE);

                        if(localimageuri != null) {
                            imageview_detailedpanel.setImageURI(localimageuri);
                        }
                        else {
                            Toast.makeText(DetailedInfoPanel.this, getString(R.string.imageunavilable), Toast.LENGTH_SHORT).show();
                        }

                        if(who != null && UserIdentifyName != null)
                        if(who.equals(UserIdentifyName)) {
                            menuItem.setVisible(true);
                        }

                        if(title != null) {
                            tv_detailedinfo_title.setText(title);
                            tv_detailedinfo_dt.setText(dt);
                            tv_detailedinfo_map.setText(map);
                            tv_detailedinfo_who.setText(who);
                        }
                        listadapter.notifyDataSetChanged();
                    }
                });
            } catch (Exception e) {
                Log.e(MainActivity.TAG, e.getMessage());
                DetailedInfoPanel.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(DetailedInfoPanel.this, "Error occued. call the developer", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }


            progressbarDialog.dismiss();

        }
    }



}
