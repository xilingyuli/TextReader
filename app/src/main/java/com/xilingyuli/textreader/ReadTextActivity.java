package com.xilingyuli.textreader;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.annotation.ColorInt;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.xilingyuli.textreader.utils.FileUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.qqtheme.framework.picker.ColorPicker;

public class ReadTextActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.text_view)
    TextView textView;
    @BindView(R.id.page)
    TextView pageView;

    String name;
    String path;
    String text;
    String[] pages;
    int currentChar;
    int currentPage;

    SharedPreferences setting,mark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_text);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        name = getIntent().getStringExtra("name");
        path = getIntent().getStringExtra("path");
        text = FileUtil.readFile(path);

        setting = getSharedPreferences("settings",MODE_PRIVATE);
        mark = getSharedPreferences(path.replace("/","_"),MODE_PRIVATE);

        currentChar = setting.getInt("CurrentChar",0);
        divideText(text,setting.getInt("FrontSize",16));
    }

    private void divideText(String text, int size){
        if(text==null||text.isEmpty())
            return;
        textView.setTextSize(size);
        textView.setText(text);
        textView.post(() -> {
            int lineNum = textView.getHeight()/textView.getLineHeight();
            int index = 0;
            int temp,i=0;
            List<String> pages1 = new ArrayList<>();
            try {
                while (index<text.length()){
                    temp = textView.getLayout().getLineStart((++i)*lineNum);
                    pages1.add(text.substring(index,temp));
                    index = temp;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            if(index<text.length())
                pages1.add(text.substring(index));
            ReadTextActivity.this.pages = pages1.toArray(new String[0]);
            gotoChar(currentChar);
        });
    }


    private int getCurrentChar(){
        currentChar = 0;
        for(int i=0;i<currentPage;i++)
            currentChar += pages[i].length();
        return currentChar;
    }

    private void gotoChar(int current)
    {
        if(current<0)
            current = 0;
        if(current>=text.length())
            current = text.length() - 1;
        for(currentPage=0;current>=0;currentPage++){
            current -= pages[currentPage].length();
        }
        currentPage--;
        jumpTo(currentPage);
    }

    public void changeFrontSize(int size){
        currentChar = getCurrentChar();
        setting.edit().putInt("FrontSize",size).apply();
        divideText(text,size);
    }

    public void changeFrontColor(@ColorInt int color){
        textView.setTextColor(color);
    }

    public void changeBackground(@ColorInt int color){
        textView.setBackgroundColor(color);
    }

    public void addBookMark(){
        int current = getCurrentChar();
        String str = text.substring(current,current+10<text.length()?current+10:text.length())
                .replaceAll("\\s+","");
        mark.edit().putString(current+"",str).apply();
    }

    public void selectBookMark(){
        Map<String,String> map = (Map<String,String>)mark.getAll();
        if(map.size()==0)
            return;
        String[] keys = new String[map.size()];
        String[] values = new String[map.size()];
        int i=0;
        for(Map.Entry<String,String> entry:map.entrySet()) {
            keys[i] = entry.getKey();
            values[i++] = entry.getValue();
        }
        new AlertDialog.Builder(this)
                .setItems(values, (dialogInterface, i1) -> gotoChar(Integer.parseInt(keys[i1])))
                .show();
    }

    public void deleteBookMark(){
        SharedPreferences.Editor editor = mark.edit();
        Map<String,String> map = (Map<String,String>)mark.getAll();
        if(map.size()==0)
            return;
        String[] keys = new String[map.size()];
        String[] values = new String[map.size()];
        boolean[] choosed = new boolean[map.size()];
        int i=0;
        for(Map.Entry<String,String> entry:map.entrySet()) {
            keys[i] = entry.getKey();
            values[i++] = entry.getValue();
        }
        new AlertDialog.Builder(this)
                .setMultiChoiceItems(values, choosed, (dialogInterface, i1, b) -> choosed[i1] = b)
                .setPositiveButton("确定", (dialogInterface, index) -> {
                    for(int i12 = 0; i12 <choosed.length; i12++)
                        if(choosed[i12])
                            editor.remove(keys[i12]);
                    editor.apply();
                })
                .setNegativeButton("取消",null)
                .show();
    }

    @OnClick(R.id.next)
    public void nextPage(){
        if(currentPage<pages.length-1)
            jumpTo(++currentPage);
        else
            Toast.makeText(this,"已是最后一页",Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.last)
    public void lastPage(){
        if(currentPage>0)
            jumpTo(--currentPage);
        else
            Toast.makeText(this,"已是第一页",Toast.LENGTH_SHORT).show();
    }

    public void jumpTo(int index){
        if(index>=0&&index<pages.length) {
            textView.setText(pages[index]);
            pageView.setText((index+1)+"/"+pages.length);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setting.edit().putInt("CurrentChar",currentChar).apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_read_text, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_front_size) {
            View view = getLayoutInflater().inflate(R.layout.dialog_choose_size,null);
            SeekBar seekBar = view.findViewById(R.id.seekBar);
            seekBar.setProgress(setting.getInt("FrontSize",16));
            new AlertDialog.Builder(this)
                    .setTitle("Text Size")
                    .setView(view)
                    .setPositiveButton("确定", (dialogInterface, i) -> changeFrontSize(seekBar.getProgress()))
                    .setNegativeButton("取消",null)
                    .show();
            return true;
        }else if(id == R.id.action_front_color){
            ColorPicker colorPicker = new ColorPicker(this);
            colorPicker.setOnColorPickListener(this::changeFrontColor);
            colorPicker.show();
        }else if(id == R.id.action_back_color){
            ColorPicker colorPicker = new ColorPicker(this);
            colorPicker.setOnColorPickListener(this::changeBackground);
            colorPicker.show();
        }
        else if(id == R.id.action_mark){
            new AlertDialog.Builder(this)
                    .setItems(new String[]{"添加书签", "选择书签", "删除书签"}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(i==0)
                                addBookMark();
                            else if(i==1)
                                selectBookMark();
                            else if(i==2)
                                deleteBookMark();
                        }
                    })
                    .show();
        }

        return super.onOptionsItemSelected(item);
    }

}
