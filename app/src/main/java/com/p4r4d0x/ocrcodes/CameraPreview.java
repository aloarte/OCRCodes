package com.p4r4d0x.ocrcodes;

/**
 * Clase que representa una SurfaceView para mostrar las imágenes de la cámara extraidas
 * Created by aloarte on 03/01/2018.
 */

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static android.content.ContentValues.TAG;
import static android.content.Context.WINDOW_SERVICE;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {


    private SurfaceHolder mHolder;

    /**
     * Objeto de la cámara
     */
    private Camera mCamera;
    private boolean isPreviewRunning;

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

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        /*
         * Para modificar la preview para primero la preview si estaba corriendo
         */
        if (isPreviewRunning) {
            mCamera.stopPreview();
        }

        /*
         * Obtiene los parámetros de la cámara , el display del dispositivo y el maximo tamaño
         * de previsualizacion de la cámara
         */
        Camera.Parameters parameters = mCamera.getParameters();
        Display display = ((WindowManager) getContext().getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Camera.Size bestSizePicture = getBestSupportedSize(parameters.getSupportedPictureSizes());
        parameters.setPictureSize(bestSizePicture.width, bestSizePicture.height);

        /*
         * Comprueba la rotación del display para aplicársela a cámara y a la preview
         */
        if (display.getRotation() == Surface.ROTATION_0) {
            parameters.setPictureSize(bestSizePicture.width, bestSizePicture.height);

            parameters.setPreviewSize(bestSizePicture.height, bestSizePicture.width);
            mCamera.setDisplayOrientation(90);
        } else if (display.getRotation() == Surface.ROTATION_90) {
            parameters.setPreviewSize(bestSizePicture.width, bestSizePicture.height);
        } else if (display.getRotation() == Surface.ROTATION_180) {
            parameters.setPreviewSize(bestSizePicture.height, bestSizePicture.width);
        } else if (display.getRotation() == Surface.ROTATION_270) {
            parameters.setPreviewSize(bestSizePicture.width, bestSizePicture.height);
            mCamera.setDisplayOrientation(180);
        }

        /*
         * Pone el autofocus
         */
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

        /*
         * La primera iteración puede lanzar una excepción.
         */
        try {

            mCamera.setParameters(parameters);

        } catch (Exception e) {
            e.printStackTrace();
        }
        previewCamera();
    }

    /**
     * Busca en la lista de los tamaños soportados por la cámara y devuelve el mejor
     *
     * @param supportedPictureSizes Lista con Camera.Size soportadas por el dispositivo
     * @return Valor de Camera.Size mas alto
     */
    private Camera.Size getBestSupportedSize(List<Camera.Size> supportedPictureSizes) {
        Iterator<Camera.Size> it = supportedPictureSizes.iterator();
        Camera.Size auxSize = null;
        int maxSizeValue = 0;
        Camera.Size maxSize = null;

        /*
         * Se recorre toda la lista en búsqueda del valor mas alto
         */
        while (it.hasNext()) {
            auxSize = it.next();
            if ((auxSize.width * auxSize.width) > maxSizeValue) {
                maxSize = auxSize;
                maxSizeValue = auxSize.width * auxSize.width;
            }
        }
        return maxSize;
    }

    /**
     * Lanza la preview de la cámara
     */
    public void previewCamera() {
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            isPreviewRunning = true;
        } catch (Exception e) {
            Log.d(CameraPreview.class.toString(), "Cannot start preview", e);
        }
    }

}