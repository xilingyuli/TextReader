package com.xilingyuli.textreader;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.xilingyuli.textreader.utils.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.qqtheme.framework.picker.FilePicker;

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
