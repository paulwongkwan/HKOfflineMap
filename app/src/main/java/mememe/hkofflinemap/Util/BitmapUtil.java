package mememe.hkofflinemap.Util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.map.android.graphics.AndroidBitmap;

/**
 * Created by Paul Wong on 17/04/25.
 */

public class BitmapUtil {

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return new AndroidBitmap(BitmapFactory.decodeResource(res, resId, options));
    }

    public static int convertDpToPixel(float dp, Context context){
        int px = (int) (dp * getDensity(context));
        return px;
    }
    /**
     * Covert px to dp
     * @param px
     * @param context
     * @return dp
     */
    public static int convertPixelToDp(float px, Context context){
        int dp = (int) (px / getDensity(context));
        return dp;
    }
    /**
     * 取得螢幕密度
     * 120dpi = 0.75
     * 160dpi = 1 (default)
     * 240dpi = 1.5
     * @param context
     * @return
     */
    public static float getDensity(Context context){
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.density;
    }

}
