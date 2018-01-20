package com.p4r4d0x.ocrcodes;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Obtiene los códigos objetivo de los parámetros de un fichero .properties
 * Created by aloarte on 16/01/2018.
 */

class ParamsConstants {

    private static final String PARAMS_RESOURCE = "TargetCodesParams.properties";

    protected static String getProperty(String key, Context context) throws IOException {
        Properties properties = new Properties();
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open(PARAMS_RESOURCE);
        properties.load(inputStream);
        return properties.getProperty(key);

    }
}
