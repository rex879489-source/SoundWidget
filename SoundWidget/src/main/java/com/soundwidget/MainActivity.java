package com.soundwidget;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int PICK = 1;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(64, 64, 64, 64);

        TextView help = new TextView(this);
        help.setTextSize(16);
        help.setText("1. Choose an audio file\n"
                + "2. Long-press your home screen > Widgets > Sound Widget\n"
                + "3. Tap the widget to play your sound");
        help.setPadding(0, 0, 0, 48);

        status = new TextView(this);
        status.setTextSize(18);
        status.setPadding(0, 0, 0, 48);

        Button pick = new Button(this);
        pick.setText("Choose audio file");
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("audio/*");
            startActivityForResult(i, PICK);
        });

        Button test = new Button(this);
        test.setText("Test play");
        test.setOnClickListener(v -> startForegroundService(new Intent(this, PlayService.class)));

        root.addView(help);
        root.addView(status);
        root.addView(pick);
        root.addView(test);
        setContentView(root);
        updateStatus();
    }

    private void updateStatus() {
        String name = getSharedPreferences("p", 0).getString("name", "none");
        status.setText("Current sound: " + name);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK || res != RESULT_OK || data == null || data.getData() == null) return;

        Uri uri = data.getData();
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(new File(getFilesDir(), "sound"))) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (Exception e) {
            Toast.makeText(this, "Could not load file", Toast.LENGTH_LONG).show();
            return;
        }

        String name = "sound";
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception ignored) { }

        getSharedPreferences("p", 0).edit().putString("name", name).apply();
        SoundWidget.refresh(this);
        updateStatus();
        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
    }
}
