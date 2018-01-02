package com.p4r4d0x.ocrcodes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    /**
     * Bitmap con la imagen a procesar
     */
    Bitmap image;

    /**
     * TextView en el que poner el resultado
     */
    TextView tvResukltOCR;

    /**
     * Api de tess-two
     */
    private TessBaseAPI mTess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initTessTwo();
    }

    /**
     * Inicializa tesseract
     */
    private void initTessTwo() {
        String datapath = getFilesDir() + "/tesseract/";
        String language = "eng";

        /*
         * Se copian los recursos de tesseract al dispositivo
         */
        copyTesseractFiles(new File(datapath + "tessdata/"), datapath);

        mTess = new TessBaseAPI();
        mTess.init(datapath, language);

    }

    /**
     * Inicializa las vistas de la actividad
     */
    private void initViews() {
        tvResukltOCR = (TextView) findViewById(R.id.OCRTextView);
        image = BitmapFactory.decodeResource(getResources(), R.drawable.test_image);

    }

    public void processImage(View view) {
        mTess.setImage(image);
        String ocrReaded = mTess.getUTF8Text();
        tvResukltOCR.setText(ocrReaded);
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

}
