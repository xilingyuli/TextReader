package com.xilingyuli.textreader;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.cos.COSClient;
import com.tencent.cos.model.COSRequest;
import com.tencent.cos.model.COSResult;
import com.tencent.cos.model.ListDirRequest;
import com.tencent.cos.model.ListDirResult;
import com.tencent.cos.model.PutObjectRequest;
import com.tencent.cos.model.UpdateObjectRequest;
import com.tencent.cos.task.listener.ICmdTaskListener;
import com.tencent.cos.task.listener.ITaskListener;
import com.tencent.cos.task.listener.IUploadTaskListener;
import com.xilingyuli.textreader.cos.CloudDataHelper;
import com.xilingyuli.textreader.cos.CloudDataUtil;
import com.xilingyuli.textreader.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.qqtheme.framework.picker.FilePicker;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.xilingyuli.textreader.cos.CloudDataHelper.ACTION_LIST_FILE;
import static com.xilingyuli.textreader.cos.CloudDataHelper.ACTION_UPLOAD_FILE;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.empty_text)
    TextView emptyText;

    SharedPreferences sharedPreferences;
    List<String[]> data = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        sharedPreferences = getSharedPreferences("booklist",MODE_PRIVATE);

        setSupportActionBar(toolbar);

        recyclerView.setLayoutManager(new GridLayoutManager(this,3));
        recyclerView.setAdapter(new BookGridAdapter());

        getBookList();
    }

    public void getBookList(){
        data.clear();
        for(Map.Entry<String, ?> entry:sharedPreferences.getAll().entrySet())
            data.add(new String[]{(String)entry.getValue(),entry.getKey()});
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.setVisibility(data.size()==0?View.GONE:View.VISIBLE);
        emptyText.setVisibility(data.size()==0?View.VISIBLE:View.GONE);
    }

    @OnClick(R.id.fab)
    public void addBooks(){
        FileUtil.requestWritePermission(this);
        FilePicker filePicker = new FilePicker(this,FilePicker.DIRECTORY);
        filePicker.setRootPath(FileUtil.ROOT_PATH);
        filePicker.getAdapter().setOnlyListDir(false);
        filePicker.setAllowExtensions(new String[]{"txt"});
        filePicker.setOnFilePickListener(currentPath -> {
            File[] files = FileUtil.listTxts(currentPath);
            if(files==null)
                return;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            for(File f : files)
                editor.putString(f.getPath(),f.getName().replace(".txt",""));
            editor.commit();
            getBookList();
        });
        filePicker.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //delete books
        if (id == R.id.action_delete) {
            if(data.size()==0)
                return true;
            String[] bookNames = new String[data.size()];
            boolean[] choosed = new boolean[bookNames.length];
            for(int i=0;i<data.size();i++)
                bookNames[i] = data.get(i)[0];
            new AlertDialog.Builder(this)
                    .setMultiChoiceItems(bookNames, choosed, (dialogInterface, i, b) -> choosed[i] = b)
                    .setPositiveButton("delete", (dialogInterface, index) -> {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        for(int i=0;i<choosed.length;i++)
                            if(choosed[i])
                                editor.remove(data.get(i)[1]);
                        editor.commit();
                        getBookList();
                    })
                    .setNegativeButton("cancel",null)
                    .show();
            return true;
        }else if(id == R.id.action_upload){
            if(data.size()==0)
                return true;
            if(toolbar.getTitle().equals("TextReader")) {
                Toast.makeText(this, "you need login!", Toast.LENGTH_SHORT);
                return true;
            }
            String[] bookNames = new String[data.size()];
            boolean[] choosed = new boolean[bookNames.length];
            for(int i=0;i<data.size();i++)
                bookNames[i] = data.get(i)[0];
            new AlertDialog.Builder(this)
                    .setMultiChoiceItems(bookNames, choosed, (dialogInterface, i, b) -> choosed[i] = b)
                    .setPositiveButton("上传", (dialogInterface, index) -> {
                        COSClient cosClient = CloudDataUtil.createCOSClient(MainActivity.this);
                        for(int i=0;i<choosed.length;i++)
                            if(choosed[i]){
                                COSRequest request = CloudDataHelper.createCOSRequest(ACTION_UPLOAD_FILE, new IUploadTaskListener() {

                                    @Override
                                    public void onSuccess(COSRequest cosRequest, COSResult cosResult) {
                                        MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this,"上传成功",Toast.LENGTH_SHORT).show());
                                    }

                                    @Override
                                    public void onFailed(COSRequest cosRequest, COSResult cosResult) {

                                    }

                                    @Override
                                    public void onProgress(COSRequest cosRequest, long l, long l1) {

                                    }

                                    @Override
                                    public void onCancel(COSRequest cosRequest, COSResult cosResult) {

                                    }
                                }, toolbar.getTitle(),new File(data.get(i)[1]));
                                cosClient.putObject((PutObjectRequest) request);
                            }

                    })
                    .setNegativeButton("取消",null)
                    .show();
            return true;
        }else if(id == R.id.action_download) {
            if(toolbar.getTitle().equals("TextReader")) {
                Toast.makeText(this, "you need login!", Toast.LENGTH_SHORT);
                return true;
            }
            COSClient cosClient = CloudDataUtil.createCOSClient(MainActivity.this);
            COSRequest request = CloudDataHelper.createCOSRequest(ACTION_LIST_FILE, new ICmdTaskListener() {
                @Override
                public void onSuccess(COSRequest cosRequest, COSResult cosResult) {
                    Gson gson = new Gson();
                    ListDirResult result = (ListDirResult)cosResult;
                    List<Map<String, String>> data = gson.fromJson(result.infos.toString(),
                            new TypeToken<List<Map<String, String>>>(){}.getType());
                    MainActivity.this.runOnUiThread(()->{
                        boolean[] choosed = new boolean[data.size()];
                        String[] names = new String[data.size()];
                        String[] urls = new String[data.size()];
                        for(int i=0;i<data.size();i++) {
                            names[i] = data.get(i).get("name");
                            urls[i] = data.get(i).get("source_url");
                        }
                        new AlertDialog.Builder(MainActivity.this)
                                .setMultiChoiceItems(names, choosed, (dialogInterface, i, b) -> choosed[i] = b)
                                .setPositiveButton("下载", (dialogInterface, index) -> {
                                    OkHttpClient client = new OkHttpClient();
                                    for(int i=0;i<choosed.length;i++) {
                                        if(!choosed[i])
                                            continue;
                                        Request request1 = new Request.Builder().url(urls[i]).build();
                                        int finalI = i;
                                        client.newCall(request1).enqueue(new Callback() {
                                            @Override
                                            public void onFailure(@NonNull Call call, @NonNull IOException e) {

                                            }

                                            @Override
                                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                                String content = response.body().string();
                                                FileUtil.saveFile(names[finalI],content);
                                                MainActivity.this.runOnUiThread(()->
                                                        Toast.makeText(MainActivity.this,names[finalI]+"已下载",Toast.LENGTH_SHORT).show());
                                            }
                                        });
                                    }
                                })
                                .setNegativeButton("取消",null)
                                .show();

                    });
                }

                @Override
                public void onFailed(COSRequest cosRequest, COSResult cosResult) {

                }
            },toolbar.getTitle(),"");
            cosClient.listDir((ListDirRequest)request);
        }else if( id == R.id.action_login){
            View view = getLayoutInflater().inflate(R.layout.dialog_login,null);
            EditText userName = view.findViewById(R.id.user_name);
            EditText password = view.findViewById(R.id.password);
            new AlertDialog.Builder(this)
                    .setView(view)
                    .setNegativeButton("register", (dialogInterface, i) -> {
                        String u = userName.getText()+"";
                        String p = password.getText()+"";
                        if(u.isEmpty()||p.isEmpty())
                        {
                            Toast.makeText(MainActivity.this,"username or password can not be empty!",Toast.LENGTH_SHORT).show();
                            return;
                        }
                        SharedPreferences preferences = getSharedPreferences("user",MODE_PRIVATE);
                        if(preferences.contains(u))
                            Toast.makeText(MainActivity.this,"用户名已存在",Toast.LENGTH_SHORT).show();
                        else {
                            preferences.edit().putString(u, p).apply();
                            toolbar.setTitle(u);
                        }
                    })
                    .setPositiveButton("login", (dialogInterface, i) -> {
                        String u = userName.getText()+"";
                        String p = password.getText()+"";
                        SharedPreferences preferences = getSharedPreferences("user",MODE_PRIVATE);
                        if(!p.isEmpty()&&preferences.getString(u,"").equals(p))
                            toolbar.setTitle(u);
                        else
                            Toast.makeText(MainActivity.this,"username or password is wrong!",Toast.LENGTH_SHORT).show();
                    })
                    .setNeutralButton("cancel",null)
                    .show();
        }

        return super.onOptionsItemSelected(item);
    }

    class BookGridAdapter extends RecyclerView.Adapter<BookGridAdapter.ViewHolder>
    {
        private LayoutInflater mInflater;

        BookGridAdapter()
        {
            mInflater = LayoutInflater.from(MainActivity.this);
        }

        @Override
        public int getItemCount()
        {
            return data.size();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i)
        {
            View view = mInflater.inflate(R.layout.item_book,
                    viewGroup, false);
            ViewHolder viewHolder = new ViewHolder(view);
            viewHolder.imageView = view.findViewById(R.id.image);
            viewHolder.textView = view.findViewById(R.id.text);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(final ViewHolder viewHolder, final int i)
        {
            viewHolder.textView.setText(data.get(i)[0]);
            viewHolder.imageView.setOnClickListener(view -> {
                Intent intent = new Intent(MainActivity.this, ReadTextActivity.class);
                intent.putExtra("name",data.get(i)[0]);
                intent.putExtra("path",data.get(i)[1]);
                startActivity(intent);
            });
        }

        class ViewHolder extends RecyclerView.ViewHolder
        {
            ViewHolder(View arg0)
            {
                super(arg0);
            }
            ImageView imageView;
            TextView textView;
        }

    }
}
