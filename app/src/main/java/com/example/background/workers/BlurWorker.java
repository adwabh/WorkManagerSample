package com.example.background.workers;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.example.background.Constants;
import com.example.background.R;

import java.io.FileNotFoundException;

import androidx.work.Data;
import androidx.work.Worker;

public class BlurWorker extends Worker {

    private static final String TAG = BlurWorker.class.getSimpleName();

    @NonNull
    @Override
    public WorkerResult doWork() {

        Context applicationContext = getApplicationContext();

        try {
            String inputUri = getInputData().getString(Constants.KEY_IMAGE_URI, null);
            int blurLevel = getInputData().getInt(Constants.KEY_BLUR_LEVEL,0);
//            Bitmap picture = BitmapFactory.decodeResource(
//                    applicationContext.getResources(),
//                    R.drawable.test);
            if(inputUri==null&& TextUtils.isEmpty(inputUri)){
                Log.e(TAG,"invalid input uri");
            }

            ContentResolver resolver = applicationContext.getContentResolver();

            Bitmap picture = BitmapFactory.decodeStream(resolver.openInputStream(Uri.parse(inputUri)));
            // Blur the bitmap
            Bitmap output = picture;
            for (int i=0;i<blurLevel;i++) {
                output = WorkerUtils.blurBitmap(output, applicationContext);
            }

            // Write bitmap to a temp file
            Uri outputUri = WorkerUtils.writeBitmapToFile(applicationContext, output);

            WorkerUtils.makeStatusNotification("Output is "
                    + outputUri.toString(), applicationContext);

            setOutputData(new Data.Builder().putString(Constants.KEY_IMAGE_URI,outputUri.toString()).build());
            // If there were no errors, return SUCCESS
            return WorkerResult.SUCCESS;
        } catch (Throwable throwable) {

            // Technically WorkManager will return WorkerResult.FAILURE
            // but it's best to be explicit about it.
            // Thus if there were errors, we're return FAILURE
            Log.e(TAG, "Error applying blur", throwable);
            return WorkerResult.FAILURE;
        }
    }


}
