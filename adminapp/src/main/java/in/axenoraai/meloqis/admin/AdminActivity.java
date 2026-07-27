package in.axenoraai.meloqis.admin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public final class AdminActivity extends Activity {
    private static final String DASHBOARD_URL =
        "https://meloqis-insights.axenora-meloqis.workers.dev/";
    private static final String DASHBOARD_HOST =
        "meloqis-insights.axenora-meloqis.workers.dev";

    private WebView webView;
    private ProgressBar progress;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_admin);

        webView = findViewById(R.id.admin_webview);
        progress = findViewById(R.id.admin_progress);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);
        settings.setSupportMultipleWindows(false);

        webView.setBackgroundColor(Color.rgb(9, 8, 13));
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                return !"https".equals(uri.getScheme()) || !DASHBOARD_HOST.equals(uri.getHost());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(
                WebView view,
                WebResourceRequest request,
                WebResourceError error
            ) {
                if (request.isForMainFrame()) {
                    progress.setVisibility(View.GONE);
                    view.loadDataWithBaseURL(
                        null,
                        "<html><meta name=\"viewport\" content=\"width=device-width\">" +
                            "<body style=\"margin:0;background:#09080d;color:#f7f4ff;" +
                            "font:16px system-ui;display:grid;place-items:center;min-height:100vh\">" +
                            "<main style=\"max-width:320px;padding:28px\"><h1>Dashboard unavailable</h1>" +
                            "<p style=\"color:#aaa4b5;line-height:1.6\">Check your connection and reopen " +
                            "Meloqis Insights Admin.</p></main></body></html>",
                        "text/html",
                        "UTF-8",
                        null
                    );
                }
            }

        });

        if (savedInstanceState == null) {
            webView.loadUrl(DASHBOARD_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                0,
                () -> {
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        finish();
                    }
                }
            );
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    @SuppressLint("GestureBackNavigation")
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
