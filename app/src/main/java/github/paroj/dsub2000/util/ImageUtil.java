package github.paroj.dsub2000.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.palette.graphics.Palette;

public class ImageUtil {

    public static Bitmap getBitmapFromDrawable(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static int getVibrantColorFromBitmap(Bitmap bitmap, int defaultColor) {
        Palette palette = Palette.from(bitmap).generate();
        int dominantColor = palette.getDominantColor(defaultColor);
        return palette.getVibrantColor(dominantColor);
    }
}
