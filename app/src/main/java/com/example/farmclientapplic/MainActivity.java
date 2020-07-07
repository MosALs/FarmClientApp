package com.example.farmclientapplic;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements PhoneNumberDialog.ExampleDialogListener{


    private Button buttonUpload,buttonSendSMS,buttonEnterPhoneNumber;
    private Uri filePath;
    private String path;
    FirebaseStorage storage;
    StorageReference storageReference;
    String fileName;
    String targetPhoneNumber = null;
    String fileData = null;

    File root = Environment.getExternalStorageDirectory();// this directory == storage/0/emulated --> after it comes all folders such as Downloads and Documents etc...
    private static final int FILE_SELECT_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        requestAppPermissions();

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        buttonUpload = findViewById(R.id.uploadbtn);
        buttonSendSMS = findViewById(R.id.sendViaSMS);
        buttonEnterPhoneNumber = findViewById(R.id.enterPhone_btn);

        if(fileData == null){
            buttonEnterPhoneNumber.setEnabled(false);
            buttonSendSMS.setEnabled(false);
            buttonEnterPhoneNumber.setBackgroundColor(Color.RED);
            buttonSendSMS.setBackgroundColor(Color.RED);
        }

        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileChooser();
            }
        });


        buttonSendSMS.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (targetPhoneNumber == null){
                    Toast.makeText(MainActivity.this, "Please, Enter phone number first", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (fileData == null){
                    Toast.makeText(MainActivity.this, "No data in the file", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendSMSMessage(targetPhoneNumber,fileData);

            }
        });

        buttonEnterPhoneNumber.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                openPhoneDialog();
            }
        });
    }

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    filePath = data.getData();
                    path = filePath.getPath();
                    System.out.println("path ========: " + path);
                    fileName = getFileName(filePath);
                    uploadFile(fileName);

                    System.out.println(fileData);


                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String readText(File file){

        StringBuilder builder = new StringBuilder();

        try{
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line ;
            while ((line = reader.readLine()) != null){
                builder.append(line);
                builder.append("\n");
            }
            reader.close();
        }catch (IOException e){
            e.printStackTrace();
            Toast.makeText(MainActivity.this,e.getCause().getMessage(),Toast.LENGTH_LONG).show();
        }
        System.out.println("builder.toString() ===: "+builder.toString());
        return builder.toString();
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }


    //this method will upload the file
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void uploadFile(String filename) {
        //if there is a file to upload
        if (filePath != null) {
            //displaying a progress dialog while upload is going on

            boolean internetConnectionFlag = checkConnectivity(MainActivity.this);

            if (internetConnectionFlag) {
                Toast.makeText(MainActivity.this, "Internet Connection successful", Toast.LENGTH_SHORT).show();
                final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setTitle("Uploading...");
                progressDialog.show();

                StorageReference riversRef = storageReference.child("files/" + filename);
                riversRef.putFile(filePath)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                //if the upload is successfull
                                //hiding the progress dialog
                                progressDialog.dismiss();

                                //and displaying a success toast
                                Toast.makeText(getApplicationContext(), "File Uploaded successfully", Toast.LENGTH_LONG).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                //if the upload is not successfull
                                //hiding the progress dialog
                                progressDialog.dismiss();

                                //and displaying error message
                                Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                //calculating progress percentage
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                                //displaying percentage in progress dialog
                                progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                            }
                        });
            } else {
                Toast.makeText(MainActivity.this, "No Internet Connection On Your Device, send file data using sms", Toast.LENGTH_LONG).show();
                File file = new File(Environment.getExternalStorageDirectory() + "/farm/upload", "test.txt");
                fileData = readText(file);
            }
        }
        //if there is not any file
        else {
            Toast.makeText(MainActivity.this, "no file found ... ", Toast.LENGTH_SHORT).show();
        }
    }



    public void sendSMSMessage(String fileData, String targetPhoneNumber) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED)
        {
            try
            {
//              this code below sends message directly without opening messages application on your phone, but to make sure you can open it and see that message has been sent successfully
                SmsManager smsMgrVar = SmsManager.getDefault();
                smsMgrVar.sendTextMessage("targetPhoneNumber", null, "fileData", null, null);


                //this code below opens messages application on your phone, waiting for your send action and you can attach a file, then send it.
//                Uri uri = Uri.parse("file://"+Environment.getExternalStorageDirectory()+"/farm/upload/test.txt");
//                Intent i = new Intent(Intent.ACTION_SEND);
//                i.putExtra("address","01014182629");
//                i.putExtra("sms_body","This is the text mms");
//                i.putExtra(Intent.EXTRA_STREAM,"file:/"+uri);
//                i.setType("text/x-vcard");
//                startActivity(i);

            }
            catch (Exception ErrVar)
            {
                Toast.makeText(getApplicationContext(),ErrVar.getMessage().toString(),
                        Toast.LENGTH_LONG).show();
                ErrVar.printStackTrace();
            }
        }
        else
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 10);
            }
        }
    }

    private void openPhoneDialog() {

        PhoneNumberDialog phoneDialog = new PhoneNumberDialog();
        phoneDialog.show(getSupportFragmentManager(), "example dialog");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean checkConnectivity(Context context) {
        String status = null;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork != null) {

//            if (activeNetwork.getType() == ConnectivityManager.) {
//                status = "Wifi enabled";
//                return status;
//            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
//                status = "Mobile data enabled";
            return true;
//            }
        } else {
            status = "No internet connection";
            return false;
        }
    }

    @Override
    public void applyTexts(String phoneNumber) {
        targetPhoneNumber = phoneNumber;
        Toast.makeText(MainActivity.this,phoneNumber,Toast.LENGTH_LONG).show();
    }
    //
    private void requestAppPermissions() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        if (hasReadPermissions() && hasWritePermissions()) {
            return;
        }

        ActivityCompat.requestPermissions(this,
                new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 3); // your request code
    }

    private boolean hasReadPermissions() {
        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED);
    }

    private boolean hasWritePermissions() {
        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED);
    }
}