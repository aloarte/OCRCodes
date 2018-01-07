package com.p4r4d0x.ocrcodes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback, AsyncProcessCode.ProcessCodeCallback {

    /**
     * Bitmap con la imagen a procesar
     */
    Bitmap image;

    /**
     * TextView en el que poner el resultado
     */
    TextView tvResukltOCR;

    /**
     * Objeto cámara
     */
    private Camera mCamera;

    /**
     * Camera preview donde se muestra la cámara
     */
    private CameraPreview mPreview;

    /**
     * Flag que controla que no se lance un asynctask adicional si ya hay uno lanzado
     */
    private boolean processingFrameLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();


        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        mCamera.setPreviewCallback(this);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    /**
     * Obtiene la instancia de la cámara
     *
     * @return objeto Camera
     */
    protected Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamera.release();
    }

    @Override
    public void onPreviewFrame(final byte[] data, Camera camera) {
        /**
         * Comprueba que no haya un frame que esté siendo procesado. En caso afirmativo, parsea
         * la imagen en un bitmap y llama a un asynctask para procesarlo con tess-two
         */
        if (!processingFrameLock) {

            processingFrameLock = true;
            Camera.Parameters parameters = camera.getParameters();
            int width = parameters.getPreviewSize().width;
            int height = parameters.getPreviewSize().height;
            YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);
            byte[] bytes = out.toByteArray();

            /*
             * Obtiene el bitmap final y llama al asynctask para ejecutar tess-two,
             * de lo contrario el hilo principal no es capaz de correr con normalidad
             */
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            new AsyncProcessCode(this, this).execute(bitmap);

        }

    }

    /**
     * Inicializa las vistas de la actividad
     */
    private void initViews() {
        tvResukltOCR = (TextView) findViewById(R.id.OCRTextView);
        image = BitmapFactory.decodeResource(getResources(), R.drawable.test_image);

    }


    @Override
    public void onProcessingFrame(ProcessStatus procStatus) {
        Log.d("ALRALR", "onProcessingFrame");
        processingFrameLock = false;
    }

    @Override
    public void onFinishProcessingFrame(String procCode) {
        Log.d("ALRALR", "onFinishProcessingFrame");

        processingFrameLock = false;
        tvResukltOCR.setText(procCode);
    }
}
