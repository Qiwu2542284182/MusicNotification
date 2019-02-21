package me.qiwu.MusicNotification;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void github(View v){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://github.com/Qiwu2542284182/MusicNotification"));
        startActivity(intent);
    }

    public void magisk(View v){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://pan.baidu.com/s/1-w5vB4Lz5NiZJFoO07dgjA"));
        startActivity(intent);
    }
}
