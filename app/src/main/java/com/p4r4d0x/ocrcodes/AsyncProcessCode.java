package com.p4r4d0x.ocrcodes;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.googlecode.tesseract.android.TessBaseAPI;

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
     * Estado alcanzado del procesamiento
     */
    private ProcessStatus procStatus = ProcessStatus.Invalid;
    /**
     * Api de tess-two
     */
    private TessBaseAPI mTess;

    public AsyncProcessCode(ProcessCodeCallback callback, TessBaseAPI mTess) {
        this.procCallback = callback;
        this.mTess = mTess;
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
        if (mTess != null) {
            mTess.setImage(bmpFrame);
            mTess.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "abcdefghijklmnñopqrstuvwxyz<>',._áéíóú\\ªº¡?¿`+´Ç¨ç^*[]{}/&%$·|@#~€");
            mTess.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789ABCDEFGHIJKLMNÑOPQRSTUVWXYZ-");
            mTess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
            return mTess.getUTF8Text();
        } else {
            return null;
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
        void onProcessingFrame(ProcessStatus procStatus);

        /**
         * Comunica la lectura exitosa de un código
         *
         * @param procCode Código leido
         */
        void onFinishProcessingFrame(String procCode);
    }
}
