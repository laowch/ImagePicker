package com.laowch.imagepicker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.List;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {


    private static final int REQUEST_CODE_TAKEN_PHOTO_GALLERY = 0x02;

    LinearLayout imageLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.add_picture).setOnClickListener(this);
        imageLayout = (LinearLayout) findViewById(R.id.image_layout);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_picture:
                onTakenGalleryPhoto();
                break;
        }
    }


    public void onTakenGalleryPhoto() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // TODO who know how to resolve that?
        // If I remove this line, the Google+ Photos will be opened prior.
        // It works well after I uninstalled Google+, but disable Google+ doesn't work.
        // So it seems like a trick made by Google+ teams.
        intent.setClass(this, ImagePickerActivity.class);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_TAKEN_PHOTO_GALLERY);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {

                case REQUEST_CODE_TAKEN_PHOTO_GALLERY: {
                    if (data != null) {
                        if (data.getParcelableArrayListExtra("uris") != null) {
                            List<Uri> uriList = data.getParcelableArrayListExtra("uris");
                            for (int i = 0; i < uriList.size(); i++) {

                                new GetBitmapFromUriTask(this, uriList.get(i), new GetBitmapFromUriTask.IOnImageTakenListener() {
                                    @Override
                                    public void onImageTaken(final Bitmap pBitmap) {
                                        MainActivity.this.onImageTaken(pBitmap);
                                    }
                                }).execute();
                            }
                        } else {
                            new GetBitmapFromUriTask(this, data.getData(), new GetBitmapFromUriTask.IOnImageTakenListener() {
                                @Override
                                public void onImageTaken(final Bitmap pBitmap) {
                                    MainActivity.this.onImageTaken(pBitmap);
                                }
                            }).execute();
                        }

                        break;
                    }
                }
            }
        }
    }

    private void onImageTaken(Bitmap pBitmap) {
        if (pBitmap == null) {
            Toast.makeText(this, "image decode error", Toast.LENGTH_LONG).show();
            return;
        }
        DisplayMetrics dm = getResources().getDisplayMetrics();

        int width = dm.widthPixels;
        int height = pBitmap.getHeight() * dm.widthPixels / pBitmap.getWidth();

        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(width, height));
        imageView.setImageBitmap(BitmapUtils.resizeBitmap(pBitmap, width, height));

        imageLayout.addView(imageView);
    }
}
