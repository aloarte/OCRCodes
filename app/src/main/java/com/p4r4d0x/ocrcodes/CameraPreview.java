package com.p4r4d0x.ocrcodes;

/**
 * Clase que representa una SurfaceView para mostrar las imágenes de la cámara extraidas
 * Created by aloarte on 03/01/2018.
 */

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

import static android.content.ContentValues.TAG;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {


    private SurfaceHolder mHolder;

    /**
     * Objeto de la cámara
     */
    private Camera mCamera;

    /**
     * Constructor completo
     *
     * @param context Contexto de ejecución
     * @param camera  Objeto cámara
     */
    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        /*
         * Obtiene el SurfaceHolder
         */
        mHolder = getHolder();
        mHolder.addCallback(this);
        // Necesario especificar el tipo en versiones anteriores a la 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    /**
     * Maneja el evento si la preview cambia o rota
     *
     * @param holder
     * @param format
     * @param w
     * @param h
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null) {
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
        }
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
}