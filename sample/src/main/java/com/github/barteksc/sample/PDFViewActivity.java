package com.github.barteksc.sample;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;
import androidx.pdf.viewer.fragment.EditablePdfViewerFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PDFViewActivity extends AppCompatActivity {

    private static final String TAG = PDFViewActivity.class.getSimpleName();

    private static final String SAMPLE_FILE = "sample.pdf";
    private static final String SAMPLE_CACHE_NAME = "sample_cached.pdf";

    private EditablePdfViewerFragment pdfFragment;
    private String pdfFileName;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        displayFromUri(uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fragmentManager = getSupportFragmentManager();
        pdfFragment = (EditablePdfViewerFragment)
                fragmentManager.findFragmentById(R.id.pdfFragmentContainer);

        if (savedInstanceState == null) {
            displaySampleAsset();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.pickFile) {
            pickFile();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void pickFile() {
        launchPicker();
    }

    private void launchPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        try {
            filePickerLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.toast_pick_file_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void displaySampleAsset() {
        try {
            File cachedFile = copyAssetToCache(SAMPLE_FILE, SAMPLE_CACHE_NAME);
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    cachedFile);
            pdfFileName = SAMPLE_FILE;
            setTitle(pdfFileName);
            pdfFragment.setDocumentUri(uri);
        } catch (IOException e) {
            Log.e(TAG, "Unable to load sample asset", e);
        }
    }

    private void displayFromUri(Uri uri) {
        pdfFileName = getFileName(uri);
        setTitle(pdfFileName);
        pdfFragment.setDocumentUri(uri);
    }

    private File copyAssetToCache(String assetFileName, String cacheFileName) throws IOException {
        File outFile = new File(getCacheDir(), cacheFileName);
        try (InputStream in = getAssets().open(assetFileName);
             OutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        return outFile;
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor =
                         getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndexOrThrow(
                            android.provider.OpenableColumns.DISPLAY_NAME);
                    result = cursor.getString(nameIndex);
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
}
