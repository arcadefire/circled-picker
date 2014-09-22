package org.angmarch.circledpicker;

/**
 * Created by Angelo Marchesin on 26/06/14.
 */

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

public class MeasureUtils {
    public static float convertDpToPixel(Context context, float densityPixel) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return densityPixel * (metrics.densityDpi / 160f);
    }
}

