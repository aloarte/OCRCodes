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
     * Relación del tamaño que ocupa el ancho del bitmap a cortar con la ROI
     */
    private static final float WIDTH_RATIO_CROP_BITMAP = 0.66F;

    /**
     * Relación del tamaño que ocupa el alto del bitmap a cortar con la ROI
     */
    private static final float HEIGHT_RATIO_CROP_BITMAP = 0.2F;

    /**
     * Relación del tamaño que ocupan los márgenes de ancho del bitmap a cortar con la ROI
     */
    private static final float WIDTH_RATIO_CROP_BITMAP_MARGIN = 0.16F;

    /**
     * Relación del tamaño que ocupan los márgenes de alto del bitmap a cortar con la ROI
     */
    private static final float HEIGHT_RATIO_CROP_BITMAP_MARGIN = 0.5F;
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
        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    /**
     * Obtiene la instancia de la cámara
     *
     * @return objeto Camera
     */
    protected Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return camera;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamera.release();
    }

    @Override
    public void onPreviewFrame(final byte[] data, Camera camera) {
        int width, height, cropAreaWidth, cropAreaHeight, cropMarginWidth, cropMarginHeight;
        Camera.Parameters parameters;
        YuvImage yuvFrame;
        ByteArrayOutputStream baosFrame;
        byte[] bytesFrame;
        Bitmap bmpFrame, bmpCroppedFrame;
        /*
         * Comprueba que no haya un frame que esté siendo procesado. En caso afirmativo, parsea
         * la imagen en un bitmap y llama a un asynctask para procesarlo con tess-two
         */
        if (!processingFrameLock) {

            processingFrameLock = true;
            parameters = camera.getParameters();
            width = parameters.getPreviewSize().width;
            height = parameters.getPreviewSize().height;
            cropAreaWidth = (int) (width * WIDTH_RATIO_CROP_BITMAP);
            cropAreaHeight = (int) (height * HEIGHT_RATIO_CROP_BITMAP);
            cropMarginWidth = (int) (width * WIDTH_RATIO_CROP_BITMAP_MARGIN);
            cropMarginHeight = (int) (height * HEIGHT_RATIO_CROP_BITMAP_MARGIN);
            yuvFrame = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);
            baosFrame = new ByteArrayOutputStream();
            yuvFrame.compressToJpeg(new Rect(0, 0, width, height), 50, baosFrame);
            bytesFrame = baosFrame.toByteArray();

            /*
             * Obtiene el bitmap final y llama al asynctask para ejecutar tess-two,
             * de lo contrario el hilo principal no es capaz de correr con normalidad
             */
            bmpFrame = BitmapFactory.decodeByteArray(bytesFrame, 0, bytesFrame.length);
            bmpCroppedFrame = Bitmap.createBitmap(bmpFrame, cropMarginWidth, cropMarginHeight, cropAreaWidth, cropAreaHeight);
            new AsyncProcessCode(this, this).execute(bmpCroppedFrame);

        }

    }

    /**
     * Inicializa las vistas de la actividad
     */
    private void initViews() {
        tvResukltOCR = findViewById(R.id.OCRTextView);
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
