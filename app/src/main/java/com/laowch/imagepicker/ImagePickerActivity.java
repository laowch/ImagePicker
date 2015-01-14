package com.laowch.imagepicker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Created by lao on 15/1/5.
 */
public class ImagePickerActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    static final String TAG = "PhotosPickerActivity";

    private static final int REQUEST_CODE_TAKEN_PHOTO_CAMERA = 0x01;

    Adapter adapter;

    DisplayImageOptions options;

    ArrayList<Uri> selected = new ArrayList<Uri>();

    GridView gridView;


    @Override
    protected void onCreate(Bundle pSavedInstanceState) {
        super.onCreate(pSavedInstanceState);


        setContentView(R.layout.activity_image_picker);

        setTitle("Pick Images");

        options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .displayer(new FadeInBitmapDisplayer(100, true, false, false))
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();


        GridView gridView = (GridView) findViewById(R.id.photo_grid);
        gridView.setOnItemClickListener(this);

        adapter = new Adapter();
        gridView.setAdapter(adapter);
        loadThumbsFromGallery();
    }


    private void loadThumbsFromGallery() {
        adapter.mContent.clear();
        Cursor imageCursor = null;
        try {
            final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.ImageColumns.ORIENTATION};
            final String orderBy = MediaStore.Images.Media.DATE_ADDED + " DESC";
            imageCursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);
            while (imageCursor.moveToNext()) {
                Uri uri = Uri.parse("file://" + imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA)));
                int orientation = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION));
                adapter.mContent.add(uri);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (imageCursor != null && !imageCursor.isClosed()) {
                imageCursor.close();
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object obj = adapter.getItem(position);
        if (obj == adapter.CAMERA) {
            final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            startActivityForResult(intent, REQUEST_CODE_TAKEN_PHOTO_CAMERA);
        } else if (obj instanceof Uri) {
            if (selected.contains(obj)) {
                selected.remove(obj);
            } else {
                selected.add((Uri) obj);
            }
            adapter.notifyDataSetChanged();
            invalidateOptionsMenu();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_image_picker, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_done).setEnabled(selected.size() > 0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_done:

                Intent intent = new Intent();
                intent.putParcelableArrayListExtra("uris", selected);
                setResult(Activity.RESULT_OK, intent);
                finish();

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_TAKEN_PHOTO_CAMERA: {
                    if (data != null) {
                        final Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                        new SaveImageAsyncTask(bitmap).execute();
                        break;
                    }
                }
            }
        }
    }

    class Adapter extends BaseAdapter {

        Object CAMERA = new Object();

        ArrayList<Uri> mContent = new ArrayList<Uri>();

        @Override
        public int getCount() {
            return mContent.size() + 1;
        }

        @Override
        public Object getItem(int position) {
            if (position == 0) {
                return CAMERA;
            } else if (position - 1 < mContent.size()) {
                return mContent.get(position - 1);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Object obj = getItem(position);
            if (obj == CAMERA) {
                ImageView imageView = new ImageView(getContext());
                int size = (DisplayUtils.getScreenWidth(getContext()) - DisplayUtils.dpToPixel(getContext(), 16)) / 3;
                imageView.setLayoutParams(new
                        AbsListView.LayoutParams(size, size));
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                imageView.setImageResource(android.R.drawable.ic_menu_camera);
                return imageView;
            } else if (obj instanceof Uri) {
                String uri = obj.toString();

                FrameLayout view = (FrameLayout) (convertView == null || !"thumb".equals(convertView.getTag()) ? LayoutInflater.from(getContext()).inflate(R.layout.item_gallery_thumbnail, null) : convertView);
                ImageView imageView = (ImageView) view.findViewById(R.id.thumb);
                if (view.getLayoutParams() == null) {
                    int size = (DisplayUtils.getScreenWidth(getContext()) - DisplayUtils.dpToPixel(getContext(), 16)) / 3;
                    view.setLayoutParams(new
                            AbsListView.LayoutParams(size, size));
                    imageView.setMaxWidth(size);
                    imageView.setMaxHeight(size);
                }
                view.setTag("thumb");


                ImageLoader.getInstance().displayImage(uri, imageView, options);

                int order = -1;
                for (int i = 0; i < selected.size(); i++) {
                    if (obj.equals(selected.get(i))) {
                        order = i + 1;
                        break;
                    }
                }

                TextView orderText = (TextView) view.findViewById(R.id.order);

                if (order < 0) {
                    orderText.setBackgroundDrawable(null);
                    orderText.setText("");
                } else {
                    orderText.setBackgroundDrawable(getResources().getDrawable(R.drawable.gallery_photo_selected));
                    orderText.setText(order + "");
                }

                return view;
            }

            return null;
        }
    }


    private class SaveImageAsyncTask extends AsyncTask<Void, Void, String> implements MediaScannerConnection.MediaScannerConnectionClient {
        private final String mPath;

        private final String mName;

        private final MediaScannerConnection mMediaScannerConnection;

        private Bitmap bitmap;

        public SaveImageAsyncTask(Bitmap bitmap) {
            this.bitmap = bitmap;

            this.mPath = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_PICTURES;
            this.mName = bitmap.hashCode() + ".png";

            this.mMediaScannerConnection = new MediaScannerConnection(ImagePickerActivity.this, this);
        }

        @Override
        protected String doInBackground(final Void... pParams) {
            File file = new File(mPath, mName);
            storeImage(bitmap, file);

            this.mMediaScannerConnection.connect();

            return file.getAbsolutePath();
        }

        @Override
        public void onMediaScannerConnected() {
            this.mMediaScannerConnection.scanFile(this.mPath + "/" + this.mName, "*/*");
        }

        @Override
        public void onScanCompleted(final String pPath, final Uri pUri) {
            this.mMediaScannerConnection.disconnect();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            gridView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadThumbsFromGallery();
                }
            }, 500);

        }
    }


    public static void storeImage(Bitmap image, File pictureFile) {
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    protected Context getContext() {
        return this;
    }
}
