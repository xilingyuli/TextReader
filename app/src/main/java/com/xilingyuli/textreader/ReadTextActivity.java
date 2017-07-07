package com.xilingyuli.textreader;

import android.content.SharedPreferences;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.xilingyuli.textreader.utils.FileUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ReadTextActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.text_view)
    TextView textView;

    String name;
    String text;
    String[] pages;
    int currentPage;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_text);
        ButterKnife.bind(this);

        sharedPreferences = getSharedPreferences("settings",MODE_PRIVATE);

        setSupportActionBar(toolbar);

        name = getIntent().getStringExtra("name");
        text = FileUtil.readFile(getIntent().getStringExtra("path"));

        currentPage = sharedPreferences.getInt("CurrentPage",0);
        divideText(text,sharedPreferences.getInt("FrontSize",16));

    }

    private void divideText(String text, int size){
        textView.setTextSize(size);
        textView.setText(text);
        textView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int lineNum = textView.getHeight()/textView.getLineHeight();
                int index = 0;
                int temp,i=0;
                List<String> pages = new ArrayList<>();
                while ((i+1)*lineNum<textView.getLineCount()){
                    temp = textView.getLayout().getLineStart((++i)*lineNum);
                    pages.add(text.substring(index,temp));
                    index = temp;
                }
                pages.add(text.substring(index));
                ReadTextActivity.this.pages = pages.toArray(new String[0]);
                if(currentPage>=ReadTextActivity.this.pages.length)
                    currentPage = ReadTextActivity.this.pages.length-1;
                jumpTo(currentPage);
                textView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    public void changeFrontSize(){

    }

    public void changeFrontColor(){

    }

    public void changeBackground(){

    }

    public void addBookMark(){

    }

    public void selectBookMark(){

    }

    @OnClick(R.id.next)
    public void nextPage(){
        if(currentPage<pages.length-1)
            jumpTo(++currentPage);
    }

    @OnClick(R.id.last)
    public void lastPage(){
        if(currentPage>0)
            jumpTo(--currentPage);
    }

    public void jumpTo(int index){
        if(index>=0&&index<pages.length)
            textView.setText(pages[index]);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPreferences.edit().putInt("CurrentPage",currentPage).apply();
    }
}
