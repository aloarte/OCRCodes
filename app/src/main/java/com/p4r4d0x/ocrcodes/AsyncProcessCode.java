package com.p4r4d0x.ocrcodes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Tarea asíncrona que procesa con tess-two un frame dado por la cámara
 * Created by aloarte on 07/01/2018.
 */

public class AsyncProcessCode extends AsyncTask<Bitmap, String, String> {

    /**
     * Calback escuchado por la actividad
     */
    private ProcessCodeCallback procCallback;
    /**
     * Contexto de ejecucución de la actividad
     */
    @SuppressLint("StaticFieldLeak")
    private Context appContext;
    /**
     * Estado alcanzado del procesamiento
     */
    private ProcessStatus procStatus = ProcessStatus.Invalid;
    /**
     * Api de tess-two
     */
    private TessBaseAPI mTess;

    public AsyncProcessCode(ProcessCodeCallback callback, Context ctx) {
        this.procCallback = callback;
        this.appContext = ctx;
    }

    @Override
    protected String doInBackground(Bitmap... bpm) {
        String codeRecognized;
        codeRecognized = processImage(bpm[0]);
        return codeRecognized;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        initTessTwo();

    }

    @Override
    protected void onPostExecute(String procCode) {
        super.onPostExecute(procCode);
        if (procCode != null) {
            procCallback.onFinishProcessingFrame(procCode);
        } else {
            procCallback.onProcessingFrame(procStatus);
        }
    }

    /**
     * Realiza el procesamiento por tesseract de un frame para extraer un código
     *
     * @param bmpFrame Bitmap a procesar
     */
    private String processImage(Bitmap bmpFrame) {
        mTess.setImage(bmpFrame);
        return mTess.getUTF8Text();
    }

    /**
     * Inicializa tesseract
     */
    private void initTessTwo() {
        String datapath = appContext.getFilesDir() + "/tesseract/";
        String language = "eng";

        /*
         * Se copian los recursos de tesseract al dispositivo
         */
        copyTesseractFiles(new File(datapath + "tessdata/"), datapath);

        mTess = new TessBaseAPI();
        mTess.init(datapath, language);

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
                    InputStream isData = appContext.getAssets().open("tessdata/eng.traineddata");
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
     * Callback para que la actividad llamante reciba los eventos del asynctask
     */
    public interface ProcessCodeCallback {
        /**
         * Escribe el mensaje de un procesamiento de un codigo
         *
         * @param procStatus Estado del procesamiento
         */
        public void onProcessingFrame(ProcessStatus procStatus);

        /**
         * Comunica la lectura exitosa de un código
         *
         * @param procCode Código leido
         */
        public void onFinishProcessingFrame(String procCode);
    }
}
