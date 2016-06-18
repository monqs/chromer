package arun.com.chromer.webheads.helper;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.graphics.Palette;

import java.lang.ref.WeakReference;

import arun.com.chromer.shared.Constants;
import arun.com.chromer.util.ColorUtil;
import arun.com.chromer.webheads.ui.WebHead;
import timber.log.Timber;

/**
 * Created by Arun on 15/05/2016.
 */
public class ColorExtractionTask extends AsyncTask<Void, Void, Integer> {

    private final WeakReference<WebHead> mWebHeadReference;
    private final WeakReference<Bitmap> mFaviconReference;

    public ColorExtractionTask(WebHead webHead, Bitmap favicon) {
        mWebHeadReference = new WeakReference<>(webHead);
        mFaviconReference = new WeakReference<>(favicon);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        int bestColor = Constants.NO_COLOR;
        if (mFaviconReference.get() != null) {
            Bitmap favicon = mFaviconReference.get();
            Palette palette = Palette.from(favicon).generate();

            bestColor = ColorUtil.getBestFaviconColor(palette);
        }

        return bestColor;
    }

    @Override
    protected void onPostExecute(Integer bestColor) {
        if (bestColor != Constants.NO_COLOR) {
            Timber.d("Got %d for favicon", bestColor);
            if (mWebHeadReference.get() != null) {
                WebHead webHead = mWebHeadReference.get();
                webHead.setWebHeadColor(bestColor);
            }
        } else Timber.e("Color extraction failed");
    }

}
