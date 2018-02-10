package com.p4r4d0x.ocrcodes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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

import static android.hardware.Camera.Parameters.FOCUS_MODE_AUTO;
import static android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;

public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback, AsyncProcessCode.ProcessCodeCallback, Camera.AutoFocusCallback {

    /**
     * Relación del tamaño que ocupa el ancho del bitmap a cortar con la ROI
     */
    private static final float WIDTH_RATIO_CROP_BITMAP = 0.56F;

    /**
     * Relación del tamaño que ocupa el alto del bitmap a cortar con la ROI
     */
    private static final float HEIGHT_RATIO_CROP_BITMAP = 0.1F;

    /**
     * Relación del tamaño que ocupan los márgenes de ancho del bitmap a cortar con la ROI
     */
    private static final float WIDTH_RATIO_CROP_BITMAP_MARGIN = 0.22F;

    /**
     * Relación del tamaño que ocupan los márgenes de alto del bitmap a cortar con la ROI
     */
    private static final float HEIGHT_RATIO_CROP_BITMAP_MARGIN = 0.5F;

    /**
     * Límite máximo de chunks que pueden tener caracteres diferentes al comparar dos códigos
     */
    private static final int MAX_DIFFERENCE_CHUNKS = 2;

    /**
     * Límite máximo de caracteres que pueden ser diferentes en un chunk al comparar dos códigos
     */
    private static final int MAX_DIFFERENCE_CHARACTERS_PER_CHUNK = 1;

    private static final int ITEM_1 = 1;
    private static final int ITEM_2 = 2;
    private static final int ITEM_3 = 3;
    private static final int ITEM_4 = 4;
    private static final int ITEM_5 = 5;
    private static final int ITEM_6 = 6;

    /**
     * Objeto cámara
     */
    private static Camera mCamera;
    /**
     * Bitmap con la imagen a procesar
     */
    Bitmap image;
    /*
     * Layout que contiene la vista de la preview de la cámara y sus elementos
     */
    RelativeLayout rlPreview;
    /**
     * Vista de la Preview de la cámara
     */
    private FrameLayout preview;
    /**
     * TextView en el que poner el resultado
     */
    private TextView tvResukltOCR;

    /**
     * TextView en el que poner la descripción del resultado
     */
    private TextView tvItemObtainedDescription;

    /**
     * ImageView con la imagen de un resultado válido
     */
    private ImageView ivItemObtainedSrc;

    /**
     * Array de iconos para mostrar los elementos obtenidos o no obtenidos aun
     */
    private ImageView[] ivItemsObtained = new ImageView[6];

    /**
     * Layout que contiene la vista del resultado de un código
     */
    private LinearLayout llResultItem;
    /**
     * Flag que controla que no se lance un asynctask adicional si ya hay uno lanzado
     */
    private boolean processingFrameLock;

    /**
     * API de tesseract
     */
    private TessBaseAPI tessTwoBaseApi;

    /**
     * SharedPreferences para almacenar y leer si un item ha sido leído por si la app se cierra.
     */
    private SharedPreferences sharedPref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        initViews();
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        checkItemValues();
        tessTwoBaseApi = initTessTwo();
    }


    @Override
    protected void onResume() {
        super.onResume();

        /*
         * Crea la instancia de la cámara, la preview y las vincula
         */
        mCamera = getCameraInstance(true, Camera.Parameters.FLASH_MODE_OFF);
        /*
      Preview de la cámara
     */
        CameraPreview cameraPreview = new CameraPreview(this, mCamera, this);
        preview.addView(cameraPreview);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamera.stopPreview();
        mCamera.setOneShotPreviewCallback(null);
        mCamera.release();
        mCamera = null;


    }

    /**
     * Recibe cada frame de la cámara
     *
     * @param data   Frame en byte[]
     * @param camera Objeto Camera
     */
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
            new AsyncProcessCode(this, tessTwoBaseApi).execute(binarizedBitmap);

        }

    }

    /**
     * Callback del asynctask de captura de código del estado del procesamiento
     *
     * @param procStatus Estado del procesamiento
     */
    @Override
    public void onProcessingFrame(ProcessStatus procStatus) {
        Log.d("ALRALR", "onProcessingFrame");
        processingFrameLock = false;
    }

    /**
     * Callback del asynctask de captura de código con el fin de un procesamiento
     *
     * @param procCode Código leido
     */
    @Override
    public void onFinishProcessingFrame(String procCode) {
        Log.d("ALRALR", "onFinishProcessingFrame");

        try {
            processingFrameLock = searchForACodeMatch(procCode);

        } catch (IOException e) {
            processingFrameLock = false;
            e.printStackTrace();
        }
    }

    @Override
    public void onAutoFocus(boolean b, Camera camera) {

    }

    /**
     * Inicializa las vistas de la actividad
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initViews() {
        preview = findViewById(R.id.camera_preview);

        tvResukltOCR = findViewById(R.id.OCRTextView);
        image = BitmapFactory.decodeResource(getResources(), R.drawable.test_image);

        tvItemObtainedDescription = findViewById(R.id.tv_achieved_element);

        llResultItem = findViewById(R.id.ll_reward);
        rlPreview = findViewById(R.id.ll_capture_code);
        ivItemObtainedSrc = findViewById(R.id.iv_item_obtained);

        /*
      Icono que activa o desactiva el flash
     */
        ImageView ivSwapFlash = findViewById(R.id.ivFlashBtn);
        /*
      Icono que realiza un enfocado de la pantalla
     */
        ImageView ivFocusScreen = findViewById(R.id.ivFocusBtn);
        ivItemsObtained[0] = findViewById(R.id.ivItem1);
        ivItemsObtained[1] = findViewById(R.id.ivItem2);
        ivItemsObtained[2] = findViewById(R.id.ivItem3);
        ivItemsObtained[3] = findViewById(R.id.ivItem4);
        ivItemsObtained[4] = findViewById(R.id.ivItem5);
        ivItemsObtained[5] = findViewById(R.id.ivItem6);

        /*
         * Botón para el reseteo de la preview tras encontrar algun código
         */
        Button btnResetPreview = findViewById(R.id.btn_reload_preview);
        btnResetPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                 * Libera el lock para que siga procesando frames y cambia las vistas
                 */
                processingFrameLock = false;
                llResultItem.setVisibility(View.GONE);
                rlPreview.setVisibility(View.VISIBLE);
                tvItemObtainedDescription.setText("");
                ivItemObtainedSrc.setBackground(getResources().getDrawable(R.drawable.rectangle));
            }
        });

        /*
         * Cuando se pulse sobre el icono, activa el flash
         */
        ivSwapFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                swapFlash(view);
            }
        });

        /*
         * Cuando se pulsa sobre el botón, hace un enfoque de la pantalla
         */
        ivFocusScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                focusCamera();
            }
        });

    }

    /**
     * Realiza un chequeo buscando si cada item se ha obtenido o no y
     * cambia su icono de forma inicial
     */
    private void checkItemValues() {
        changeItemIconStatus(ITEM_1, sharedPref.getBoolean(getString(R.string.savedPreferenceItem1), false));
        changeItemIconStatus(ITEM_2, sharedPref.getBoolean(getString(R.string.savedPreferenceItem2), false));
        changeItemIconStatus(ITEM_3, sharedPref.getBoolean(getString(R.string.savedPreferenceItem3), false));
        changeItemIconStatus(ITEM_4, sharedPref.getBoolean(getString(R.string.savedPreferenceItem4), false));
        changeItemIconStatus(ITEM_5, sharedPref.getBoolean(getString(R.string.savedPreferenceItem5), false));
        changeItemIconStatus(ITEM_6, sharedPref.getBoolean(getString(R.string.savedPreferenceItem6), false));

    }

    /**
     * Cambia el icono del item seleccionado en función del valor booleano
     *
     * @param itemIndex Índice del item a evaluar
     * @param obtained  Condición que evalua si esta obtenido o no
     */
    public void changeItemIconStatus(int itemIndex, boolean obtained) {
        switch (itemIndex) {
            case ITEM_1:
                if (obtained)
                    ivItemsObtained[0].setBackground(getResources().getDrawable(R.drawable.ic_item_1_white));
                else
                    ivItemsObtained[0].setBackground(getResources().getDrawable(R.drawable.ic_item_1_black));
                break;
            case ITEM_2:
                if (obtained)
                    ivItemsObtained[1].setBackground(getResources().getDrawable(R.drawable.ic_item_2_white));
                else
                    ivItemsObtained[1].setBackground(getResources().getDrawable(R.drawable.ic_item_2_black));
                break;
            case ITEM_3:
                if (obtained)
                    ivItemsObtained[2].setBackground(getResources().getDrawable(R.drawable.ic_item_3_white));
                else
                    ivItemsObtained[2].setBackground(getResources().getDrawable(R.drawable.ic_item_3_black));
                break;
            case ITEM_4:
                if (obtained)
                    ivItemsObtained[3].setBackground(getResources().getDrawable(R.drawable.ic_item_4_white));
                else
                    ivItemsObtained[3].setBackground(getResources().getDrawable(R.drawable.ic_item_4_black));
                break;
            case ITEM_5:
                if (obtained)
                    ivItemsObtained[4].setBackground(getResources().getDrawable(R.drawable.ic_item_5_white));
                else
                    ivItemsObtained[4].setBackground(getResources().getDrawable(R.drawable.ic_item_5_black));
                break;
            case ITEM_6:
                if (obtained)
                    ivItemsObtained[5].setBackground(getResources().getDrawable(R.drawable.ic_item_6_white));
                else
                    ivItemsObtained[5].setBackground(getResources().getDrawable(R.drawable.ic_item_6_black));
                break;
        }
        rlPreview.invalidate();
    }

    /**
     * Obtiene la instancia de la cámara
     *
     * @return objeto Camera
     */
    protected Camera getCameraInstance(boolean autofocus, String flashMode) {
        Camera camera = null;
        Camera.Parameters parameters;
        try {
            int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            camera = Camera.open(cameraId);
            parameters = camera.getParameters();
            /*
             * Si se activó el autofocus, le configura un modo de autofocus que tenga el dispositivo
             */
            if (autofocus) {
                List<String> focusModes = parameters.getSupportedFocusModes();
                if (focusModes.contains(FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.setFocusMode(FOCUS_MODE_CONTINUOUS_PICTURE);
                } else if (focusModes.contains(FOCUS_MODE_AUTO)) {
                    parameters.setFocusMode(FOCUS_MODE_AUTO);
                }
            }
            /*
             * Si se activó el flash, se configura el parámetro
             */
            if (parameters.getFlashMode() != null) {
                parameters.setFlashMode(flashMode);
            }
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return camera;
    }

    /**
     * Activa o desactiva el flash en función de su estado anterior
     */
    private void swapFlash(View v) {
        Camera.Parameters params = mCamera.getParameters();
        try {
            if (params.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                v.setBackground(getResources().getDrawable(R.drawable.ic_flash_off));
            } else {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                v.setBackground(getResources().getDrawable(R.drawable.ic_flash_on));
            }
            mCamera.stopPreview();
            mCamera.setParameters(params);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        if (copyTesseractFiles(new File(datapath + "tessdata/"), datapath)) {
            TessBaseAPI mTess = new TessBaseAPI();
            mTess.init(datapath, language);
            return mTess;
        } else {
            return null;
        }




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

    /**
     * Busca si el código reconocido por tesseract concuerda con alguno de los codigos objetivo
     *
     * @param procCode Código reconocído por OCR
     * @return True si el string coincidió con el de referencia.
     */
    private boolean searchForACodeMatch(String procCode) throws IOException {

        /*
         * Comprueba que el string no esté vacío o nulo
         */
        if (procCode != null && !procCode.equals("")) {
            String improvedCode = improveRecognizedCode(procCode);
            SharedPreferences.Editor editor = sharedPref.edit();

            boolean obtained;


            if (compareCodes(ParamsConstants.getProperty("instrCode", this), improvedCode, MAX_DIFFERENCE_CHARACTERS_PER_CHUNK, MAX_DIFFERENCE_CHUNKS)) {
                obtained = true;
                llResultItem.setVisibility(View.VISIBLE);
                rlPreview.setVisibility(View.GONE);
                tvItemObtainedDescription.setText(getResources().getString(R.string.descr_instr));
                ivItemObtainedSrc.setBackground(getResources().getDrawable(R.drawable.almohada));

            } else if (compareCodes(ParamsConstants.getProperty("codeCard1", this), improvedCode, MAX_DIFFERENCE_CHARACTERS_PER_CHUNK - 1, MAX_DIFFERENCE_CHUNKS - 1)) {
                obtained = true;
                llResultItem.setVisibility(View.VISIBLE);
                rlPreview.setVisibility(View.GONE);
                tvItemObtainedDescription.setText(getResources().getString(R.string.descr_item1));
                ivItemObtainedSrc.setBackground(getResources().getDrawable(R.drawable.caja));
                changeItemIconStatus(ITEM_1, true);
                editor.putBoolean(getString(R.string.savedPreferenceItem1), true);
                editor.apply();

            } else if (compareCodes(ParamsConstants.getProperty("codeGame2", this), improvedCode, MAX_DIFFERENCE_CHARACTERS_PER_CHUNK, MAX_DIFFERENCE_CHUNKS)) {
                obtained = true;
                llResultItem.setVisibility(View.VISIBLE);
                rlPreview.setVisibility(View.GONE);
                tvItemObtainedDescription.setText(getResources().getString(R.string.descr_item2));
                ivItemObtainedSrc.setBackground(getResources().getDrawable(R.drawable.mochila));
                changeItemIconStatus(ITEM_2, true);
                editor.putBoolean(getString(R.string.savedPreferenceItem2), true);
                editor.apply();

            } else if (compareCodes(ParamsConstants.getProperty("codeGame1", this), improvedCode, MAX_DIFFERENCE_CHARACTERS_PER_CHUNK + 1, MAX_DIFFERENCE_CHUNKS + 1)) {
                obtained = true;
                llResultItem.setVisibility(View.VISIBLE);
                rlPreview.setVisibility(View.GONE);
                tvItemObtainedDescription.setText(getResources().getString(R.string.descr_item3));
                ivItemObtainedSrc.setBackground(getResources().getDrawable(R.drawable.maletero));
                changeItemIconStatus(ITEM_3, true);
                editor.putBoolean(getString(R.string.savedPreferenceItem3), true);
                editor.apply();

            } else if (compareCodes(ParamsConstants.getProperty("codeCard2", this), improvedCode, MAX_DIFFERENCE_CHARACTERS_PER_CHUNK + 1, MAX_DIFFERENCE_CHUNKS + 1)) {
                obtained = true;
                llResultItem.setVisibility(View.VISIBLE);
                rlPreview.setVisibility(View.GONE);
                tvItemObtainedDescription.setText(getResources().getString(R.string.descr_item4));
                ivItemObtainedSrc.setBackground(getResources().getDrawable(R.drawable.maleta));
                changeItemIconStatus(ITEM_4, true);
                editor.putBoolean(getString(R.string.savedPreferenceItem4), true);
                editor.apply();

            } else if (compareCodes(ParamsConstants.getProperty("paperCode1", this), improvedCode, MAX_DIFFERENCE_CHARACTERS_PER_CHUNK + 1, MAX_DIFFERENCE_CHUNKS + 1)) {
                obtained = true;
                llResultItem.setVisibility(View.VISIBLE);
                rlPreview.setVisibility(View.GONE);
                tvItemObtainedDescription.setText(getResources().getString(R.string.descr_item5));
                ivItemObtainedSrc.setBackground(getResources().getDrawable(R.drawable.cama));
                changeItemIconStatus(ITEM_5, true);
                editor.putBoolean(getString(R.string.savedPreferenceItem5), true);
                editor.apply();

            } else if (compareCodes(ParamsConstants.getProperty("paperCode2", this), improvedCode, MAX_DIFFERENCE_CHARACTERS_PER_CHUNK + 1, MAX_DIFFERENCE_CHUNKS + 2)) {
                obtained = true;
                llResultItem.setVisibility(View.VISIBLE);
                rlPreview.setVisibility(View.GONE);
                tvItemObtainedDescription.setText(getResources().getString(R.string.descr_item6));
                ivItemObtainedSrc.setBackground(getResources().getDrawable(R.drawable.mochila2));
                changeItemIconStatus(ITEM_6, true);
                editor.putBoolean(getString(R.string.savedPreferenceItem6), true);
                editor.apply();

            } else if (compareCodes(ParamsConstants.getProperty("trollCode1", this), improvedCode, MAX_DIFFERENCE_CHARACTERS_PER_CHUNK, MAX_DIFFERENCE_CHUNKS)) {
                obtained = false;
                llResultItem.setVisibility(View.VISIBLE);
                rlPreview.setVisibility(View.GONE);
                tvItemObtainedDescription.setText(getResources().getString(R.string.descr_troll_item1));
                ivItemObtainedSrc.setBackground(getResources().getDrawable(R.drawable.baiabaia));

            } else if (compareCodes(ParamsConstants.getProperty("trollCode2", this), improvedCode, MAX_DIFFERENCE_CHARACTERS_PER_CHUNK, MAX_DIFFERENCE_CHUNKS)) {
                obtained = false;
                llResultItem.setVisibility(View.VISIBLE);
                rlPreview.setVisibility(View.GONE);
                tvItemObtainedDescription.setText(getResources().getString(R.string.descr_troll_item2));
                ivItemObtainedSrc.setBackground(getResources().getDrawable(R.drawable.hand));
            } else {
                obtained = false;
            }
            return obtained;
        } else {
            return false;
        }

    }

    /**
     * Compara dos códigos: el objetivo y el reconocido. Se relaja la comprobación para permitir
     * que un numero determinado de caracteres no coincida.
     *
     * @param targetCode                Código contra el que matchear
     * @param recognizedCode            Código a comprobar
     * @param differenceCharactersLimit Numero máximo de caracteres por chunk que se permite que difieran
     * @param maxErrorChunks            Número maximo de chunks que puedan tener differenceCharactersLimit diferentes
     * @return True si coincide bajo las restricciones, False en caso contrario
     */
    private boolean compareCodes(String targetCode, String recognizedCode, int differenceCharactersLimit, int maxErrorChunks) {
        /*
         * Comprueba si tienen la misma longitud
         */
        if (recognizedCode.length() == targetCode.length()) {
            String[] chunksRecognizedCode = recognizedCode.split("-");
            String[] chunksTargetCode = targetCode.split("-");
            /*
             * El número de chunks debe coincidir
             */
            if (chunksRecognizedCode.length == chunksTargetCode.length) {
                int errorCharactersInChunk = 0;
                for (int i = 0; i < chunksRecognizedCode.length; i++) {
                    /*
                     * La longitud de los chunks debe coincidir
                     */
                    if (chunksRecognizedCode[i].length() == chunksTargetCode[i].length()) {
                        /*
                         * Compara cada chunk del código reconocido con cada chunk del código objetivo
                         */
                        int errorCharacters = 0;
                        for (int j = 0; j < chunksRecognizedCode[i].length(); j++) {
                            if (chunksRecognizedCode[i].charAt(j) != chunksTargetCode[i].charAt(j)) {
                                errorCharacters++;
                            }
                        }
                        /*
                         * Comprueba que no haya mas errores en el chunk que el permitido
                         */
                        if (errorCharacters > differenceCharactersLimit) {
                            return false;
                        }
                        /*
                         * Si hubo algún error en el chunk, se marca como un chunk con error
                         */
                        if (errorCharacters > 0) {
                            errorCharactersInChunk++;
                        }
                        /*
                         * Si en algún punto el error de chunks con fallo supera el límite,
                         * se toma como inválido
                         */
                        if (errorCharactersInChunk > maxErrorChunks) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
                /*
                 * Si ha llegado hasta este punto, el string es o igual, o muy parecido al objetivo
                 */
                return true;
            } else {
                return false;
            }

        } else {
            return false;
        }

    }

    /**
     * Realiza una mejora del código para adecuarse al objetivo
     *
     * @param procCode Código a mejorar
     * @return Código mejorado
     */
    private String improveRecognizedCode(String procCode) {
        String retProcCode;
        /*
         * Elimina el caracter '|', '\' y '/'
         */
        retProcCode = procCode.replace("|", "").replace("\\", "").replace("/", "");
        /*
         * Elimina cuando se encuentre caracteres del limite del frame
         */
        retProcCode = retProcCode.replace("I ", "").replace(" I", "");
        retProcCode = retProcCode.replace("l ", "").replace(" l", "");
        /*
         * Elimina todos los espacios
         */
        retProcCode = retProcCode.replace(" ", "");

        tvResukltOCR.setText(retProcCode);

        return retProcCode;

    }

}
