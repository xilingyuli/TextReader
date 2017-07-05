package com.xilingyuli.textreader;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final int CHOOSE_BOOKS_DIR = 571;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.empty_text)
    TextView emptyText;

    List<String[]> data = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        recyclerView.setLayoutManager(new GridLayoutManager(this,3));
        recyclerView.setAdapter(new BookGridAdapter());

        getBookList("");
    }

    public void getBookList(String path){
        //TODO
        recyclerView.setVisibility(data.size()==0?View.GONE:View.VISIBLE);
        emptyText.setVisibility(data.size()==0?View.VISIBLE:View.GONE);
    }

    @OnClick(R.id.fab)
    public void addBooks(){
        Intent intent = new Intent();
        //TODO
        startActivityForResult(intent,CHOOSE_BOOKS_DIR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==CHOOSE_BOOKS_DIR&&resultCode==RESULT_OK){
            getBookList(data.getStringExtra("path"));
        }
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
        if (id == R.id.action_settings) {
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
            viewHolder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, ReadTextActivity.class);
                    intent.putExtra("path",data.get(i)[1]);
                    startActivity(intent);
                }
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
