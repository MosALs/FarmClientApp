package com.example.farmclientapplic;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class MainActivity extends AppCompatActivity {


    private Button buttonUpload , buttonChooseFile;
    private Button buttonDownload;
    private TextView filenametxt , fileDatatxt;
    private Uri filePath;
    private String path;
    FirebaseStorage storage;
    StorageReference storageReference;
    String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        

    }
}