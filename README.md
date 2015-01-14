It's ImagePicker which implements the effect and UI like Tumblr app. 

And it also provides content for Intent.ACTION_GET_CONTENT, which means other Apps can use it as ImagePicker.

To use it as ImagePicker, you can add code like this :

    // choose image button click
    public void onClick(View v) {
       final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // If I remove this line, the Google+ Photos will be opened prior.
        // It works well after I uninstalled Google+, but disable Google+ doesn't work.
        // So it seems like a trick made by Google+ teams.
        intent.setClassName("com.laowch.imagepicker", "com.laowch.imagepicker.ImagePickerActivity");
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_CODE_TAKEN_PHOTO_GALLERY);
    }


And as result, it returns image uris which you selected in Intent, and you can get the List<Uri> like this:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        List<Uri> uriList = data.getParcelableArrayListExtra("uris");
    }
