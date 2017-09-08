package me.zeyuan.lib.autopreferences;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {
    private TextView label;
    private Button putButton;
    private Button getButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        getButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AuthPreferences.getUserName(MainActivity.this);
                label.setText("Version:" + SettingPreferences.getVersion(MainActivity.this));
            }
        });
        putButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingPreferences.setVersion(MainActivity.this, 100);
            }
        });
    }

    private void initView() {
        label = (TextView) findViewById(R.id.label);
        putButton = (Button) findViewById(R.id.put);
        getButton = (Button) findViewById(R.id.get);
    }
}
