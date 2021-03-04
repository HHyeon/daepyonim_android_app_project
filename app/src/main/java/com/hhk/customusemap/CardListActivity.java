package com.hhk.customusemap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.hhk.customusemap.MainActivity.ACTIVITY_REQUESTCODE_MISCINFOSHOWPANEL;
import static com.hhk.customusemap.MainActivity.INTENT_DATA_IDENTIFYNAME;
import static com.hhk.customusemap.MainActivity.INTENT_DATA_ITEM_ID;

public class CardListActivity extends AppCompatActivity {

    static final String INTENT_DATA_FROM_CARDLIST_ID = "INTENT_DATA_FROM_CARDLIST_ID";
    static final String INTENT_DATA_FROM_CARDLIST_TITLE = "INTENT_DATA_FROM_CARDLIST_TITLE";
    static final String INTENT_DATA_FROM_CARDLIST_WHO = "INTENT_DATA_FROM_CARDLIST_WHO";
    static final String INTENT_DATA_FROM_CARDLIST_MAP = "INTENT_DATA_FROM_CARDLIST_MAP";
    static final String INTENT_DATA_FROM_CARDLIST_DT = "INTENT_DATA_FROM_CARDLIST_DT";
    static final String INTENT_DATA_FROM_CARDLIST_LAT = "INTENT_DATA_FROM_CARDLIST_LAT";
    static final String INTENT_DATA_FROM_CARDLIST_LON = "INTENT_DATA_FROM_CARDLIST_LON";

//    private Parcelable viewstateposition = null;
    private RecyclerView recyclerView;
    private MapLocationListAdapter listAdapter;
    private SwipeRefreshLayout swipelayout;
    private TextView tv_display_no_data;
    private String UserIdentifyName = null;
    private SimpleLoadingProgressbarDialog progressbarDialog;
    private int page_index = 0;
    private int page_firid = 0;
    private boolean nextpage_available = true;

    LinearLayoutManager linearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_list);

        swipelayout = findViewById(R.id.swipelayout);
        recyclerView = findViewById(R.id.recyclerview);
        tv_display_no_data = findViewById(R.id.tv_display_no_data);
        tv_display_no_data.setVisibility(View.INVISIBLE);

        swipelayout.setOnRefreshListener(refreshListener);

        linearLayoutManager = new LinearLayoutManager(this);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if(!recyclerView.canScrollVertically(1) && recyclerView.getScrollState() != 0)  { // bottom reached && user moved
                    Log.d(MainActivity.TAG, "lowest scroll reach !!!");
                    nextlisting();
                }
            }
        });

        listAdapter = new MapLocationListAdapter();
        recyclerView.setAdapter(listAdapter);

        UserIdentifyName = getIntent().getStringExtra(INTENT_DATA_IDENTIFYNAME);
        Log.d(MainActivity.TAG, "UserIdentifyName : " + UserIdentifyName);

        DBserverworker runner = new DBserverworker();
        runner.start();
    }

    SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            resetlisting();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ACTIVITY_REQUESTCODE_MISCINFOSHOWPANEL) {
            resetlisting();
        }
    }

    void resetlisting() {
        Log.d(MainActivity.TAG, "RESET LIST !!!!!!");
        page_index = 0;
        page_firid = 0;
        nextpage_available = true;
        tv_display_no_data.setVisibility(View.INVISIBLE);
        listAdapter.ClearAllItem();
        listAdapter.notifyDataSetChanged();
        DBserverworker runner = new DBserverworker();
        runner.start();
    }

    void nextlisting() {
        Log.d(MainActivity.TAG, "NEXT LIST !!!!!!");
        if(nextpage_available) {
            page_index++;
            DBserverworker runner = new DBserverworker();
            runner.start();
        }
        else {
            Toast.makeText(CardListActivity.this, getString(R.string.listactivity_alldata_showed), Toast.LENGTH_SHORT).show();
            Log.d(MainActivity.TAG, "all data showed");
        }

        Log.d(MainActivity.TAG, "page_index : " + page_index + ", page_firid : " + page_firid + ", nextpage_available : " + nextpage_available);
    }

    static class CardListItem {
        private String id;
        private String map;
        private String title;
        private String dt;
        private String who;
        private double lat;
        private double lon;
        private Uri imagefileuri;

        CardListItem(String id, String title, String dt, String map, String who, double lat, double lon, @Nullable Uri uri) {
            this.id = id;
            this.title = title;
            this.dt = dt;
            this.map = map;
            this.who = who;
            this.lat = lat;
            this.lon = lon;
            this.imagefileuri = uri;
        }

        String getTitle() {
            return title;
        }

        String getDt() {
            return dt;
        }

        String getWho() {
            return who;
        }

        String getId() {
            return id;
        }

        String getMap() {
            return map;
        }

        double getLat() {
            return lat;
        }

        double getLon() {
            return lon;
        }

        Uri getimageFileUri() {
            return imagefileuri;
        }
    }

    class MapLocationListAdapter extends RecyclerView.Adapter<MapLocationListAdapter.CustomViewHolder> {

        private List<CardListItem> listItems;

        MapLocationListAdapter() {
            listItems = new ArrayList<CardListItem>();
        }

        void ClearAllItem() {
            listItems.clear();
        }

        void AddItem(String id, String title, String dt,String map, String who, double lat, double lon, Uri uri) {
            CardListItem item = new CardListItem(id, title, dt, map, who, lat, lon,uri);
            listItems.add(item);
        }

        @NonNull
        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_item_layout,parent,false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomViewHolder holder, final int position) {
            final CardListItem item = listItems.get(position);

            if(item.getimageFileUri() != null)
                holder.imageview_carditem.setImageURI(item.getimageFileUri());
            else
                holder.imageview_carditem.setImageResource(R.drawable.ic_photo_white_24dp);

            holder.tv_cardview_title.setText(item.getTitle());
            holder.tv_cardview_info_dt.setText(item.getDt());
            holder.tv_cardview_info_who.setText(item.getWho());
            if(item.getWho().equals(UserIdentifyName))
                holder.tv_myuploads.setVisibility(View.VISIBLE);
            else
                holder.tv_myuploads.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return listItems.size();
        }

        class CustomViewHolder extends RecyclerView.ViewHolder {
            ImageView imageview_carditem;
            ImageButton btn_detailinfoshow;
            TextView tv_cardview_title, tv_cardview_info_dt,tv_cardview_info_who, tv_myuploads;
            CustomViewHolder(View v) {
                super(v);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getAdapterPosition();
                        if(position != RecyclerView.NO_POSITION) {
                            final CardListItem item = listItems.get(position);

                            // Not Really Necessary whole things
                            Intent intent = new Intent();
                            intent.putExtra(INTENT_DATA_FROM_CARDLIST_ID, item.getId());
                            intent.putExtra(INTENT_DATA_FROM_CARDLIST_TITLE, item.getTitle());
                            intent.putExtra(INTENT_DATA_FROM_CARDLIST_WHO, item.getWho());
                            intent.putExtra(INTENT_DATA_FROM_CARDLIST_MAP, item.getMap());
                            intent.putExtra(INTENT_DATA_FROM_CARDLIST_DT, item.getDt());
                            intent.putExtra(INTENT_DATA_FROM_CARDLIST_LAT, item.getLat());
                            intent.putExtra(INTENT_DATA_FROM_CARDLIST_LON, item.getLon());
                            setResult(Activity.RESULT_OK, intent);
                            finish();

                        }
                    }
                });
                imageview_carditem = v.findViewById(R.id.imageview_carditem);
                tv_cardview_title = v.findViewById(R.id.tv_cardview_title);
                tv_cardview_info_dt = v.findViewById(R.id.tv_cardview_info_dt);
                tv_cardview_info_who = v.findViewById(R.id.tv_cardview_info_who);
                tv_myuploads = v.findViewById(R.id.tv_myuploads);
                btn_detailinfoshow = v.findViewById(R.id.btn_detailinfoshow);

                btn_detailinfoshow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getAdapterPosition();
                        if(position != RecyclerView.NO_POSITION) {
                            CardListItem item = listItems.get(position);
                            Intent intent = new Intent(CardListActivity.this.getApplicationContext(), DetailedInfoPanel.class);
                            intent.putExtra(INTENT_DATA_ITEM_ID, Integer.parseInt(item.getId()));
                            intent.putExtra(INTENT_DATA_IDENTIFYNAME, UserIdentifyName);
                            startActivityForResult(intent, ACTIVITY_REQUESTCODE_MISCINFOSHOWPANEL);

//                            viewstateposition = linearLayoutManager.onSaveInstanceState();
                        }
                    }
                });
            }
        }
    }

    private class DBserverworker extends Thread {

        DBserverworker() {
            progressbarDialog = new SimpleLoadingProgressbarDialog(CardListActivity.this);
            progressbarDialog.show();
        }

        private String requestdata() throws Exception {
            HttpURLConnection connection;
            URL url = new URL(getString(R.string.Server_Inet_ADDR)+"?method=1&page="+page_index+"&firid="+page_firid); // method=1 is select action
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
            boolean success = false;
            FTPClient ftpClient = new FTPClient();
            try {
                String jsonraw = requestdata();
                ftpClient.connect(getString(R.string.FtpServerAddress), 16021);
                ftpClient.login("hyeon", "001123");
                ftpClient.changeWorkingDirectory("uplodedimages");
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                ftpClient.enterRemotePassiveMode();
                ftpClient.enterLocalPassiveMode();

                JSONObject jsonObjectSrc;
                jsonObjectSrc = new JSONObject(jsonraw);

                String nowpagecount = jsonObjectSrc.getString("work");

                if(nowpagecount.equals("empty")) {
                    nextpage_available = false;
                    Log.d(MainActivity.TAG, "no available item now");
                    if(page_index == 0) {
                        Log.d(MainActivity.TAG, "empty when pageindex 0 => set visible nodata text");
                        CardListActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv_display_no_data.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }
                else {
                    int availableitemcnt = Integer.parseInt(nowpagecount);
                    if(availableitemcnt < 10) {
                        nextpage_available = false;
                        Log.d(MainActivity.TAG, "this is last page");
                    }

                    JSONArray resultdata = jsonObjectSrc.getJSONArray("result");
                    for(int i=0;i<resultdata.length();i++) {
                        JSONObject resulteach = resultdata.getJSONObject(i);
                        final String itemid = resulteach.getString("id");
                        String title = resulteach.getString("title");
                        String dt = resulteach.getString("dt");
                        String map = resulteach.getString("map");
                        String who = resulteach.getString("who");
                        double lat = resulteach.getDouble("lat");
                        double lon = resulteach.getDouble("lon");
                        if(page_index == 0 && i == 0) {
                            page_firid = Integer.parseInt(itemid);
                            Log.d(MainActivity.TAG, "page_firid set : " + page_firid);
                        }


                        Uri localimageuri = null;
                        File workdir = getCacheDir();
                        String[] nameslist = workdir.list(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                return name.contains(itemid + ".");
                            }
                        });

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
                        listAdapter.AddItem(itemid,title,dt,map,who,lat,lon,localimageuri);
                    }
                }
                success = true;
            } catch (Exception e) {
                Log.e(MainActivity.TAG, e.getMessage());
                e.printStackTrace();
                success = false;
            }
            finally {
                try {
                    ftpClient.disconnect();
                } catch (IOException ignored) {
                }
                final boolean finalSuccess = success;
                CardListActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(finalSuccess) {
                            listAdapter.notifyDataSetChanged();
                        }
                        else {
                            Toast.makeText(CardListActivity.this, getString(R.string.ERRORS), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                swipelayout.setRefreshing(false);
                progressbarDialog.dismiss();
            }
        }
    }
}