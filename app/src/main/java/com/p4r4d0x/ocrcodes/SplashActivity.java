package com.p4r4d0x.ocrcodes;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class SplashActivity extends AppCompatActivity {

    /**
     * Constante de la request de permisos
     */
    private static final int REQUEST_PERMISSION = 1;

    /**
     * Flag que controla si el permiso de la camara se ha habilitado
     */
    private boolean cameraPermission = false;

    /**
     * Flag que controla si el permiso de lectura y escritura se ha habilitado
     */
    private boolean readWritePermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        /*
         * Comprueba los permisos e inicia las vistas
         */
        checkPermissions();
        initViews();

    }

    /**
     * Inicia las vistas de la actividad
     */
    private void initViews() {
        Button btnContinueMain = findViewById(R.id.btn_continue_main);
        btnContinueMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraPermission && readWritePermission) {
                    Intent activityIntent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(activityIntent);
                    finish();
                } else {
                    checkPermissions();
                }

            }
        });
    }


    /**
     * Comprueba si estan los permisos necesarios concedidos. En caso contrario los pide al usuario
     */
    private void checkPermissions() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        } else {
            readWritePermission = true;
            cameraPermission = true;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION: {
                cameraPermission = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                readWritePermission = grantResults.length > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED;
            }
        }
    }


}
