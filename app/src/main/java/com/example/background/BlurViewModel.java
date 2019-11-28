/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.net.Uri;
import android.text.TextUtils;

import com.example.background.workers.BlurWorker;
import com.example.background.workers.CleanupWorker;
import com.example.background.workers.SaveImageToFileWorker;

import java.util.List;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import static com.example.background.Constants.TAG_OUTPUT;

public class BlurViewModel extends ViewModel {

    private final LiveData<List<WorkInfo>> mSavedWorkStatus;
    private WorkManager mWorkManager;
    private Uri mImageUri;
    private Uri mOutputImageUri;

    public BlurViewModel() {
        mWorkManager = WorkManager.getInstance();
        mSavedWorkStatus = mWorkManager.getWorkInfosByTagLiveData(TAG_OUTPUT);
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     *
     * @param blurLevel The amount to blur the image
     */
    void applyBlur(int blurLevel) {

        Constraints constraints = new Constraints.Builder()
                .setRequiresCharging(true)
                .build();

        OneTimeWorkRequest cleanUpRequest = new OneTimeWorkRequest.Builder(CleanupWorker.class)
                .setConstraints(constraints)
                .build();

        OneTimeWorkRequest blurRequest = new OneTimeWorkRequest.Builder(BlurWorker.class)
                .setInputData(createInputDataForUri(blurLevel))
                .setConstraints(constraints)
                .build();

        OneTimeWorkRequest saveRequest = new OneTimeWorkRequest.Builder(SaveImageToFileWorker.class)
                .addTag(TAG_OUTPUT)
                .build();
        WorkManager.getInstance().beginUniqueWork(Constants.IMAGE_MANIPULATION_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                cleanUpRequest)
        .then(blurRequest)
        .then(saveRequest)
        .enqueue();
    }

    private Uri uriOrNull(String uriString) {
        if (!TextUtils.isEmpty(uriString)) {
            return Uri.parse(uriString);
        }
        return null;
    }

    /**
     * Setters
     */
    void setImageUri(String uri) {
        mImageUri = uriOrNull(uri);
    }

    /**
     * Getters
     */
    Uri getImageUri() {
        return mImageUri;
    }

    Data createInputDataForUri(int blurLevel) {
        Data.Builder builder = new Data.Builder();
        if (mImageUri != null) {
            builder.putString(Constants.KEY_IMAGE_URI, mImageUri.toString());
            builder.putInt(Constants.KEY_BLUR_LEVEL,blurLevel);
        }
        return builder.build();
    }

    public LiveData<List<WorkInfo>> getSavedWorkStatus() {
        return mSavedWorkStatus;
    }

    public Uri getOutputImageUri() {
        return mOutputImageUri;
    }
}