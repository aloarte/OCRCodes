package com.p4r4d0x.ocrcodes;

/*
 * Clase que representa una SurfaceView para mostrar las imágenes de la cámara extraidas
 * Created by aloarte on 03/01/2018.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

import static android.content.ContentValues.TAG;

@SuppressLint("ViewConstructor")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {


    SurfaceHolder mHolder;
    /**
     * Objeto de la cámara
     */
    private Camera mCamera;
    private PreviewCallback previewCallback = null;

    /**
     * Constructor completo
     *
     * @param context Contexto de ejecución
     * @param camera  Objeto cámara
     */
    public CameraPreview(Context context, Camera camera, Camera.PreviewCallback previewCallback) {
        super(context);
        this.mCamera = camera;
        this.previewCallback = previewCallback;
        mHolder = getHolder();
        mHolder.addCallback(this);
        // Necesario especificar el tipo en versiones anteriores a la 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if (mCamera != null) {
            try {
                if (previewCallback != null) {
                    mCamera.setPreviewCallback(previewCallback);
                }
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();

            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() != null && mCamera != null) {
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
            surfaceCreated(holder);
        }
    }
}