package kg.dos2.taxi_client;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.mikhaellopez.circularimageview.CircularImageView;

import java.io.InputStream;

public class ImageUtils {
    public static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        CircularImageView bmImage;
        ImageView imageView;
        int code = 0;

        public DownloadImageTask(CircularImageView bmImage) {
            this.bmImage = bmImage;
            code = 1;
        }

        public DownloadImageTask(ImageView iv) {
            imageView = iv; code = 0;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            if ( code == 0 && imageView != null ) {
                imageView.setImageBitmap(result);
            } else if ( code == 1 && bmImage != null ) {
                bmImage.setImageBitmap(result);
            }
        }
    }
}
