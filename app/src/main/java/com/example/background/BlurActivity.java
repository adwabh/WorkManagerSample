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

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;

import com.bumptech.glide.Glide;

import java.util.List;

import androidx.work.Data;
import androidx.work.WorkManager;
import androidx.work.WorkStatus;


public class BlurActivity extends AppCompatActivity {

    private BlurViewModel mViewModel;
    private ImageView mImageView;
    private ProgressBar mProgressBar;
    private Button mGoButton, mOutputButton, mCancelButton;
    private Uri mFinalOutputUri;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blur);

        // Get the ViewModel
        mViewModel = ViewModelProviders.of(this).get(BlurViewModel.class);

        // Get all of the Views
        mImageView = findViewById(R.id.image_view);
        mProgressBar = findViewById(R.id.progress_bar);
        mGoButton = findViewById(R.id.go_button);
        mOutputButton = findViewById(R.id.see_file_button);
        mCancelButton = findViewById(R.id.cancel_button);

        // Image uri should be stored in the ViewModel; put it there then display
        Intent intent = getIntent();
        String imageUriExtra = intent.getStringExtra(Constants.KEY_IMAGE_URI);
        mViewModel.setImageUri(imageUriExtra);
        if (mViewModel.getImageUri() != null) {
            Glide.with(this).load(mViewModel.getImageUri()).into(mImageView);
        }
        setUpProgressHandler(mViewModel);

        // Setup blur image file button
        mGoButton.setOnClickListener(view -> mViewModel.applyBlur(getBlurLevel()));
        mOutputButton.setOnClickListener(view -> {invokeActionView(mFinalOutputUri);});
        mCancelButton.setOnClickListener(view -> {cancelTask();});
    }

    private void cancelTask() {
        WorkManager.getInstance().cancelUniqueWork(Constants.IMAGE_MANIPULATION_WORK_NAME);
    }

    private void invokeActionView(Uri currentUri) {
        if (currentUri!=null) {
            Intent intent = new Intent(Intent.ACTION_VIEW,currentUri);
            if (intent.resolveActivity(getPackageManager())!=null) {
                startActivity(intent);
            }
        }
    }

    private void setUpProgressHandler(BlurViewModel viewModel) {
        viewModel.getSavedWorkStatus().observe(this, new Observer<List<WorkStatus>>() {
            @Override
            public void onChanged(@Nullable List<WorkStatus> workStatuses) {
                if(workStatuses == null || workStatuses.isEmpty())
                    return;
                WorkStatus status = workStatuses.get(0);
                if(status.getState().isFinished()){
                    showWorkFinished();
                    mOutputButton.setVisibility(View.VISIBLE);
                    Data data = status.getOutputData();
                    String stringFinal = data.getString(Constants.KEY_IMAGE_URI, null);
                    if (stringFinal!=null&& !TextUtils.isEmpty(stringFinal)) {
                        mFinalOutputUri = Uri.parse(stringFinal);
                    }
                } else {
                    mOutputButton.setVisibility(View.GONE);
                   showWorkInProgress();
                }
            }
        });
    }

    /**
     * Shows and hides views for when the Activity is processing an image
     */
    private void showWorkInProgress() {
        mProgressBar.setVisibility(View.VISIBLE);
        mCancelButton.setVisibility(View.VISIBLE);
        mGoButton.setVisibility(View.GONE);
        mOutputButton.setVisibility(View.GONE);
    }

    /**
     * Shows and hides views for when the Activity is done processing an image
     */
    private void showWorkFinished() {
        mProgressBar.setVisibility(View.GONE);
        mCancelButton.setVisibility(View.GONE);
        mGoButton.setVisibility(View.VISIBLE);
    }

    /**
     * Get the blur level from the radio button as an integer
     * @return Integer representing the amount of times to blur the image
     */
    private int getBlurLevel() {
        RadioGroup radioGroup = findViewById(R.id.radio_blur_group);

        switch(radioGroup.getCheckedRadioButtonId()) {
            case R.id.radio_blur_lv_1:
                return 1;
            case R.id.radio_blur_lv_2:
                return 2;
            case R.id.radio_blur_lv_3:
                return 3;
        }

        return 1;
    }
}