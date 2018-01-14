package com.p4r4d0x.ocrcodes;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback, AsyncProcessCode.ProcessCodeCallback, Camera.AutoFocusCallback {

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
     * Preview de la cámara
     */
    private FrameLayout preview;

    /**
     * Camera preview donde se muestra la cámara
     */
    private CameraPreview mPreview;

    /**
     * Flag que controla que no se lance un asynctask adicional si ya hay uno lanzado
     */
    private boolean processingFrameLock;

    /**
     * API de tesseract
     */
    private TessBaseAPI tessTwoBaseApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();

        /**
         * Crea la instancia de la cámara, la preview y las vincula
         */
        mCamera = getCameraInstance();
        mPreview = new CameraPreview(this, mCamera);
        mCamera.setPreviewCallback(this);
        preview.addView(mPreview);

        tessTwoBaseApi = initTessTwo();
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
        mCamera.stopPreview();
        mCamera.setOneShotPreviewCallback(null);
        mCamera = null;
        mCamera.release();

    }

    @Override
    public void onPreviewFrame(final byte[] data, Camera camera) {
        int width, height, cropAreaWidth, cropAreaHeight, cropMarginWidth, cropMarginHeight;
        Camera.Parameters parameters;
        YuvImage yuvFrame;
        ByteArrayOutputStream baosFrame;
        byte[] bytesFrame;
        Bitmap bmpFrame, bmpCroppedFrame, binarizedBitmap;
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
            Bitmap bmp = bmpCroppedFrame;
            binarizedBitmap = BitmapUtils.binarizeBitmap(bmp);
            new AsyncProcessCode(this, this, tessTwoBaseApi).execute(binarizedBitmap);

        }

    }

    /**
     * Inicializa las vistas de la actividad
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initViews() {
        preview = findViewById(R.id.camera_preview);

        tvResukltOCR = findViewById(R.id.OCRTextView);
        image = BitmapFactory.decodeResource(getResources(), R.drawable.test_image);

        /*
         * Cuando se toca sobre la superficie de la pantalla se hace autofocus
         */
        preview.setClickable(true);
        preview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                focusCamera();
                return true;
            }
        });

    }

    /**
     * Realiza un enfocado d ela imágen de la cámara
     */
    public void focusCamera() {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            /*
             * Cancela el autofocus y obtiene la lista de focus posibles para asignarle el automatico
             */
            mCamera.cancelAutoFocus();
            List<String> autoFocusModes = params.getSupportedFocusModes();
            if (autoFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mCamera.setParameters(params);
            }

            /*
             * Realiza el autofocus
             */
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCamera.autoFocus(MainActivity.this);
                }
            }, 50);

            mCamera.autoFocus(this);
        }
    }

    /**
     * Inicializa tesseract
     */
    private TessBaseAPI initTessTwo() {
        String datapath = getFilesDir() + "/tesseract/";
        String language = "eng";

        /*
         * Se copian los recursos de tesseract al dispositivo
         */
        copyTesseractFiles(new File(datapath + "tessdata/"), datapath);

        TessBaseAPI mTess = new TessBaseAPI();
        mTess.init(datapath, language);

        return mTess;

    }

    /**
     * Obtiene de la carpeta de assets el fichero eng.traineddata y lo copia en el móvil para
     * que tesseract pueda usarlo
     */
    private boolean copyTesseractFiles(File directory, String dataPath) {
         /*
         * El path del fichero completo
         */
        String filepath = dataPath + "/tessdata/eng.traineddata";

        /*
         * Comprueba que el directorio exista
         */
        if ((!directory.exists() && directory.mkdirs()) || directory.exists()) {
            File datafile = new File(filepath);
            /*
             * Comprueba que el fichero no exista ya previamente para no volver a crearlo
             */
            if (!datafile.exists()) {
                try {

                    /*
                     * Se obtiene el asset eng.traineddata y se prepara para escribirlo en el directorio
                     */
                    InputStream isData = getAssets().open("tessdata/eng.traineddata");
                    OutputStream osData = new FileOutputStream(filepath);

                    /*
                     * Se escribe el fichero
                     */
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = isData.read(buffer)) != -1) {
                        osData.write(buffer, 0, read);
                    }
                    osData.flush();
                    osData.close();
                    isData.close();
                    return true;

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return false;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            /*
             * Si el fichero ya existía, no se vuelve a crear
             */
            else {
                return true;
            }
        } else {
            return false;
        }

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

    @Override
    public void onAutoFocus(boolean b, Camera camera) {

    }
}
