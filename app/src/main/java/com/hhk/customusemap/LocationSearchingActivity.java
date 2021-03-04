package com.hhk.customusemap;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class LocationSearchingActivity extends AppCompatActivity {

    static public final String INTENT_DATA_LATITUDE = "INTENT_DATA_LATITUDE";
    static public final String INTENT_DATA_LONGITUDE = "INTENT_DATA_LONGITUDE";
    static public final String INTENT_DATA_MAPADDRNAME = "INTENT_DATA_MAPADDRNAME";
    static public final String INTENT_DATA_MAPADDRTYPE = "INTENT_DATA_MAPADDRTYPE";

    EditText edit_location_search_input;
    SearchResultListViewAdapter searchResultListViewAdapter;
    ListView listview_location_search;
    TextView tv_total_item_count;
    ProgressBar searchingactivity_progressbar;
    maprestapidataretrieverunner runner = null;
    boolean lastitemVisibleFlag = false;


    SimpleLoadingProgressbarDialog progressbarDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_searching);
        layout();
        setResult(Activity.RESULT_CANCELED);
    }

    void layout() {
        searchingactivity_progressbar = findViewById(R.id.searchingactivity_progressbar);
        edit_location_search_input = findViewById(R.id.edit_location_search_input);
        listview_location_search = findViewById(R.id.listview_location_search);
        tv_total_item_count = findViewById(R.id.tv_total_item_count);

        edit_location_search_input.setOnEditorActionListener(editorActionListener);
        listview_location_search.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE && lastitemVisibleFlag) {
                    if(runner != null) {
                        runner.processs();
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                lastitemVisibleFlag = (totalItemCount > 0) && (firstVisibleItem + visibleItemCount >= totalItemCount);
            }
        });

        listview_location_search.setOnItemClickListener(listviewclicklistener);

        Toolbar toolbar = findViewById(R.id.toolbar_searchingactivity);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

//        ActionBar actionBar = getSupportActionBar();
//        assert actionBar != null;
//        actionBar.setDisplayHomeAsUpEnabled(true);
//        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
    }

    AdapterView.OnItemClickListener listviewclicklistener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            SearchResultListItem item = (SearchResultListItem)searchResultListViewAdapter.getItem(position);
            Intent intent = new Intent();
            intent.putExtra(INTENT_DATA_LATITUDE, item.getMapAddressLatitude());
            intent.putExtra(INTENT_DATA_LONGITUDE, item.getMapAddressLongitude());
            intent.putExtra(INTENT_DATA_MAPADDRNAME, item.getMapAddressName());
            intent.putExtra(INTENT_DATA_MAPADDRTYPE, item.getMapAddressType());
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    TextView.OnEditorActionListener editorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if(actionId == EditorInfo.IME_ACTION_SEARCH) {
                String str = edit_location_search_input.getText().toString();
                runner = new maprestapidataretrieverunner(str);
                runner.processs();
                return true;
            }
            return false;
        }
    };

    private class maprestapidataretrieverunner {
        private int foundlocationinfototal;
        private int resultcount;
        private String searchkey;
        private int currentloadingpage;
        private boolean nextpageavailable;
        private int increase = 0;

        maprestapidataretrieverunner(String searchkey) {
            this.searchkey = searchkey;
            currentloadingpage = 1;
            resultcount = 0;
            nextpageavailable = true;
            searchResultListViewAdapter = new SearchResultListViewAdapter();
            listview_location_search.setAdapter(searchResultListViewAdapter);
        }

        private class thisthread extends Thread {
            @Override
            public void run() {
                if(!nextpageavailable) {
                    Log.d(MainActivity.TAG, "All Data Retrieved");
                }
                else {
                    LocationSearchingActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            searchingactivity_progressbar.setVisibility(View.VISIBLE);
                        }
                    });

                    try{
                        String jsonStrSrc = getJsonStringfromSearched(currentloadingpage);

                        Log.d("json", jsonStrSrc);

                        JSONObject jsonObjectSrc;
                        jsonObjectSrc = new JSONObject(jsonStrSrc);
                        JSONObject mapmetadata = jsonObjectSrc.getJSONObject("meta");
                        foundlocationinfototal = mapmetadata.getInt("total_count");

                        JSONArray json_data_field = jsonObjectSrc.getJSONArray("documents");
                        for(int i=0;i<json_data_field.length();i++) {
                            JSONObject lowerobject = json_data_field.getJSONObject(i);
                            String address_name = lowerobject.getString("address_name");
                            String address_type = lowerobject.getString("address_type");
                            double address_latitude = lowerobject.getDouble("y");
                            double address_longitude = lowerobject.getDouble("x");
                            if(address_type.equals("REGION"))
                                address_type = "지명";
                            else if(address_type.equals("ROAD"))
                                address_type = "도로명";
                            else if(address_type.equals("REGION_ADDR"))
                                address_type = "지번 주소";
                            else if(address_type.equals("ROAD_ADDR"))
                                address_type = "도로명 주소";
                            address_name += "," + increase++;
    //                        Log.d(MainActivity.TAG, (i+1)+":"+ address_name);
                            searchResultListViewAdapter.addItem(
                                    address_name,address_type,
                                    address_latitude, address_longitude);
                        }

                        resultcount += json_data_field.length();
                        if(foundlocationinfototal <= resultcount)  {
                            nextpageavailable = false;
                            Log.d(MainActivity.TAG, "max data reached");
                        }

    //                    Log.d(MainActivity.TAG, "total " + resultcount + " Items");

                        LocationSearchingActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String str = foundlocationinfototal + " 건 찾음";
                                tv_total_item_count.setText(str);
                                searchResultListViewAdapter.notifyDataSetChanged();
                                searchingactivity_progressbar.setVisibility(View.GONE);
                            }
                        });

                        currentloadingpage++;

                    }catch (IOException e) {
                        Log.e(MainActivity.TAG, e.getMessage());
                    } catch (JSONException e) {
                        Log.e(MainActivity.TAG, e.getMessage());
                    }
                }

                progressbarDialog.dismiss();
            }
        }

        void processs() {
            progressbarDialog = new SimpleLoadingProgressbarDialog(LocationSearchingActivity.this);
            progressbarDialog.show();
            thisthread thread = new thisthread();
            thread.start();
        }

        private String getJsonStringfromSearched(int page) throws IOException {
            final int onepagesize = 20;
            HttpURLConnection conn;
            URL url = new URL("https://dapi.kakao.com/v2/local/search/address.json?size="+ onepagesize +"&page="+page+"&query=" + searchkey);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "KakaoAK " + MainActivity.RESTAPIKEY);
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String str;
            StringBuilder buffer = new StringBuilder();
            while((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
            }
//            Log.d(MainActivity.TAG, "result :"+buffer.toString());
            return buffer.toString();
        }
    }





    private static class SearchResultListViewAdapter extends BaseAdapter {
        private ArrayList<SearchResultListItem> searchResultListItems = new ArrayList<SearchResultListItem>();

        @Override
        public int getCount() {
            return searchResultListItems.size();
        }

        @Override
        public Object getItem(int position) {
            return searchResultListItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.searchresult_listitem_layout, parent, false);
            }

            TextView tv_addr_name = convertView.findViewById(R.id.searchresultlistlayout_addressname);
            TextView tv_addr_type = convertView.findViewById(R.id.searchresultlistlayout_addresstype);

            SearchResultListItem item = searchResultListItems.get(position);

            tv_addr_name.setText(item.getMapAddressName());
            tv_addr_type.setText(item.getMapAddressType());

            return convertView;
        }

        void addItem(String name, String type, double lat, double lon) {
            SearchResultListItem item = new SearchResultListItem(name,type, lat, lon);
            searchResultListItems.add(item);
        }
    }
    private static class SearchResultListItem {
        private String MapAddressName;
        private String MapAddressType;
        private double MapAddressLatitude;
        private double MapAddressLongitude;

        SearchResultListItem(String name, String type, double lat, double lon) {
            MapAddressName = name;
            MapAddressType = type;
            MapAddressLatitude = lat;
            MapAddressLongitude = lon;
        }

        String getMapAddressName() {
            return MapAddressName;
        }

        String getMapAddressType() {
            return MapAddressType;
        }

        double getMapAddressLatitude() {
            return MapAddressLatitude;
        }

        double getMapAddressLongitude() {
            return MapAddressLongitude;
        }
    }

}
