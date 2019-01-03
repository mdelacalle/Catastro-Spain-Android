package com.brownietech.catastro;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

public class WebViewActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        Intent intent = getIntent();
        String url = intent.getStringExtra("URL");

       final ProgressBar pb = findViewById(R.id.progressBar);

        WebView wb = findViewById(R.id.web_view);

        wb.setWebChromeClient(new WebChromeClient(){

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                pb.setProgress(newProgress);

                if(newProgress==100){
                    findViewById(R.id.progressLayout).setVisibility(View.GONE);
                }
            }
        });

        wb.getSettings().setJavaScriptEnabled(true);
        wb.loadUrl(url);

        findViewById(R.id.closing_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }
}
