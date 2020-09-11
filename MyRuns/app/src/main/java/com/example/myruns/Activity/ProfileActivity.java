package com.example.myruns.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.myruns.BuildConfig;
import com.example.myruns.Fragment.MyRunsDialogFragment;
import com.example.myruns.R;
import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ProfileActivity extends AppCompatActivity {

    private static final String PROFILE_GENDER = "gender";
    private static final String PROFILE_NAME = "name";
    public static final String PROFILE_EMAIL = "email";
    public static final String PROFILE_PASSWORD = "password";
    private static final String PROFILE_MAJOR = "major";
    private static final String PROFILE_PHONE = "phone";
    private static final String PROFILE_CLASS = "class";
    private static final int REQUEST_SUBMITTED = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final int REQUEST_TAKE_PHOTO= 4;
    private static final int REQUEST_CHOOSE_PHOTO = 5;
    private static final String PROFILE_URI = "profile uri";
    private static final String PROFILE_MALE = "male";
    private static final String PROFILE_FEMALE = "female";
    private static final String PROFILE_NB = "nonbin";
    private static final int REQUEST_SUBMITTED_UPDATE = 6;
    public static final String FROM_PROFILE = "fromProfile";
    private Button mChangePhotoButton;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mPreferenceEditor;
    private String mCurrentPhotoPath;
    private File mPhoto;
    private Uri mImageCaptureUri;
    private ImageView mImageView;
    private Bitmap rotatedBitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        mChangePhotoButton = findViewById(R.id.change_photo);

        mSharedPreferences = getSharedPreferences("ProfileData", MODE_PRIVATE);
        mPreferenceEditor = mSharedPreferences.edit();
        mImageView = findViewById(R.id.profile_image);

        if(savedInstanceState != null){
            mImageCaptureUri = savedInstanceState.getParcelable("outputFileUri");
            mImageView.setImageURI(mImageCaptureUri);
        } else {
            mImageCaptureUri = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(getResources().getResourcePackageName(R.drawable.blue_heart))
                    .appendPath(getResources().getResourcePackageName(R.drawable.blue_heart))
                    .appendPath(getResources().getResourcePackageName(R.drawable.blue_heart))
                    .build();
        }

        // If from an intent
        Intent intent = getIntent();
        if (intent != null) {
            if(intent.getBooleanExtra(MainActivity.KEY_FROM_MAIN, false)) {
                loadProfile();
            }
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("outputFileUri", mImageCaptureUri);
    }

    public void takeProfilePhoto(int option)  {
        Intent intent;

        // Depends on gallery/ taking photo
        switch(option) {

            case MyRunsDialogFragment.TAKE_PHOTO:
                // Photo from camera
                intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                // File to store the photo
                try {
                    mPhoto = createImageFile();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
                // Continue if file created successfully
                if (mPhoto != null) {
                    mImageCaptureUri= FileProvider.getUriForFile(this,
                            BuildConfig.APPLICATION_ID,
                            mPhoto);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri);

                    // Only run intent if there is a valid fill
                }
                try {
                    startActivityForResult(intent, REQUEST_TAKE_PHOTO);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
                break;

            case MyRunsDialogFragment.CHOOSE_PHOTO:
                Log.d("Work", "hello");
                intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);

                // File to store the photo
                try {
                    mPhoto = createImageFile();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }

                if (mPhoto != null) {
                    Uri photoURI = FileProvider.getUriForFile(this,
                            BuildConfig.APPLICATION_ID,
                            mPhoto);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    // Only run intent if there is a valid fill
                }
                //intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
                try {
                    startActivityForResult(intent, REQUEST_CHOOSE_PHOTO);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
        }
    }

    public void onChangePhoto(View v) throws IOException {
        checkPermissions();

        MyRunsDialogFragment fragment = MyRunsDialogFragment.newInstance("photo");
        fragment.show(getSupportFragmentManager(), getString(R.string.profile_photo));
    }


    private void startCrop(Uri source) {

        Log.d("mPhoto", mPhoto.toString());
        if (mPhoto != null) {
            Uri destination = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, mPhoto);
            Log.d("Womp", "start Crop");
            Crop.of(source, destination).asSquare().start(this);
        }
        Log.d("Womp", "start Crop failed");
    }
    private void updateCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {

            mImageCaptureUri = Crop.getOutput(result);
            mImageView.setImageURI(mImageCaptureUri);
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            Log.d("womp1", "result_ok");
            return;
        }

        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
                // Start cropping
                Bitmap rotatedRotatedBitmap = imageOrient(mPhoto);
                try{
                    FileOutputStream fout = new FileOutputStream(mPhoto);
                    rotatedRotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fout);
                    fout.flush();
                    fout.close();
                } catch(IOException e){

                }
                startCrop(mImageCaptureUri);
                break;
            case REQUEST_CHOOSE_PHOTO:
                Log.d("womp1", "choose photo");
                mImageCaptureUri = data.getData();
                Log.d("womp2", mImageCaptureUri.toString());
                startCrop(mImageCaptureUri);
                break;
            case Crop.REQUEST_CROP:
                Log.d("womp1", "crop request");
                //update crop
                updateCrop(resultCode, data);
        }
    }

    private Bitmap imageOrient(File photoFile) {
        ExifInterface ei;
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), FileProvider.getUriForFile(this,
                    BuildConfig.APPLICATION_ID,
                    photoFile));
            ei = new ExifInterface(photoFile.getAbsolutePath());
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            rotatedBitmap = null;
            switch (orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(bitmap, 90);

                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(bitmap, 180);

                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(bitmap, 270);

                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    rotatedBitmap = bitmap;

                    break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return rotatedBitmap;
    }

    private Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }


    private File createImageFile() throws IOException {
        //time
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }



    public boolean isEmailValid (String email_attempt) {
        return (!TextUtils.isEmpty(email_attempt) && Patterns.EMAIL_ADDRESS.matcher(email_attempt).matches());
    }

    public boolean isNameValid(String name_attempt){
        return ((!TextUtils.isEmpty(name_attempt)) && name_attempt.matches("^[\\p{L} .'-]+$"));
    }

    public boolean isClassYearValid(String year_attempt_string){
        Integer year_attempt;
        if (TextUtils.isEmpty(year_attempt_string)) return false;
        try { year_attempt = Integer.parseInt(year_attempt_string); }
        catch (NumberFormatException ex) { return false; }
        return ((year_attempt > 1900) && (year_attempt < 2100));
    }

    public boolean isMajorValid(String name_attempt){
        return (!TextUtils.isEmpty(name_attempt) && name_attempt.matches("^[\\p{L} .'-]+$"));
    }

    public boolean isPasswordValid(String password_attempt){
        return (!TextUtils.isEmpty(password_attempt) && password_attempt.length() > 5);
    }

    public boolean isPhoneValid (String phone_attempt) {
        return (!TextUtils.isEmpty(phone_attempt) && Patterns.PHONE.matcher(phone_attempt).matches());
    }


    private void checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return;
        }


        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 0);
        }
    }

    public void onSubmitClicked(View v) {
        View focusView = null;
        boolean cancel = false;
        int fields = 7;
        boolean passwordChanged = false;

        // Gender Buttons
        RadioGroup radioGroup = findViewById(R.id.gender_radio_group);
        RadioButton maleButton = findViewById(R.id.male_button);
        RadioButton femaleButton = findViewById(R.id.female_button);
        RadioButton mLastRadioBtn = findViewById(R.id.nonbinary_button);
        int selectedId = radioGroup.getCheckedRadioButtonId();
        boolean maleButtonChecked = maleButton.isChecked();
        boolean femaleButtonChecked = femaleButton.isChecked();
        boolean nbinaryButtonChecked = mLastRadioBtn.isChecked();

        mLastRadioBtn.setError(null);
        // Name
        EditText mNameView = findViewById(R.id.name);
        String name = mNameView.getText().toString();
        mNameView.setError(null);
        // Email
        EditText mEmailView = findViewById(R.id.email_address);
        String email = mEmailView.getText().toString();
        mEmailView.setError(null);
        // Password
        EditText mPasswordView = findViewById(R.id.password);
        String password = mPasswordView.getText().toString();
        mPasswordView.setError(null);
        // Major
        EditText mMajorView = findViewById(R.id.major);
        String major = mMajorView.getText().toString();
        mMajorView.setError(null);
        // Major
        EditText mPhoneView = findViewById(R.id.phone_number);
        String phone = mPhoneView.getText().toString();
        mPhoneView.setError(null);
        // Class Year
        EditText mClassYear = findViewById(R.id.class_year);
        String classYear = mClassYear.getText().toString();
        mMajorView.setError(null);

        // Gender
        if (selectedId <= 0) {
            focusView = mLastRadioBtn;
            mLastRadioBtn.setError(getString(R.string.error_gender));
            fields--;
        }
        // Name
        if (!isNameValid(name)) {
            mNameView.setError(getString(R.string.error_invalid_name));
            focusView = mNameView;
            fields--;
        }
        if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            fields--;
        }
        // Password
        if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            fields--;
        }
        //
        if (!isMajorValid(major)) {
            mMajorView.setError(getString(R.string.error_invalid_major));
            focusView = mMajorView;
            fields--;
        }
        // Phone
        if (!isPhoneValid(phone)) {
            mPhoneView.setError(getString(R.string.error_invalid_phone));
            focusView = mPhoneView;
            fields--;
        }
        // Class Year
        if (!isClassYearValid(classYear)) {
            mClassYear.setError(getString(R.string.error_invalid_class));
            focusView = mClassYear;
            fields--;
        }

        // Password Change Check
        if (!password.equals(mSharedPreferences.getString(PROFILE_PASSWORD, ""))) {
            passwordChanged = true;
        }

        // Photo Check
        if (mImageCaptureUri == null) {

        }

        if (fields == 7) {
            //gender
            mPreferenceEditor.putBoolean(PROFILE_MALE, maleButtonChecked);
            mPreferenceEditor.putBoolean(PROFILE_FEMALE, femaleButtonChecked);
            mPreferenceEditor.putBoolean(PROFILE_NB, nbinaryButtonChecked);
            mPreferenceEditor.putString(PROFILE_NAME, name);
            mPreferenceEditor.putString(PROFILE_EMAIL, email);
            mPreferenceEditor.putString(PROFILE_PASSWORD, password);
            mPreferenceEditor.putString(PROFILE_MAJOR, major);
            mPreferenceEditor.putString(PROFILE_PHONE, phone);
            mPreferenceEditor.putString(PROFILE_CLASS, classYear);
            mPreferenceEditor.putString(PROFILE_URI, mImageCaptureUri.toString());
            mPreferenceEditor.commit();

            if (getIntent().getBooleanExtra(MainActivity.KEY_FROM_MAIN, false)) {
                if (!passwordChanged) {
                    Intent intentMain = new Intent(ProfileActivity.this, MainActivity.class);
                    startActivityForResult(intentMain, REQUEST_SUBMITTED);
                }
                else {
                    finish();
                    Intent intentSignIn = new Intent(ProfileActivity.this, SignInActivity.class);
                    intentSignIn.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intentSignIn.putExtra(FROM_PROFILE, true);
                    startActivityForResult(intentSignIn, REQUEST_SUBMITTED_UPDATE);
                }
            }
            else {
                // Default or updated Password
                finish();
                Intent intentSignIn = new Intent(ProfileActivity.this, SignInActivity.class);
                intentSignIn.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivityForResult(intentSignIn, REQUEST_SUBMITTED_UPDATE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_DENIED || grantResults[1] == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                        || shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Show an explanation to the user asychronously
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("This permission is important for the app.").setTitle("Important permission required");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 0);
                        }
                    });
                    builder.show();
                }
            }
        }
    }

    private void loadProfile() {

        // Photo
        mImageCaptureUri =  Uri.parse(mSharedPreferences.getString(PROFILE_URI, ""));
        mImageView.setImageURI(mImageCaptureUri);

        // Gender Button
        RadioButton maleButton = findViewById(R.id.male_button);
        RadioButton femaleButton = findViewById(R.id.female_button);
        RadioButton nbinaryButton = findViewById(R.id.nonbinary_button);
        maleButton.setChecked(mSharedPreferences.getBoolean(PROFILE_MALE, false));
        femaleButton.setChecked(mSharedPreferences.getBoolean(PROFILE_FEMALE, false));
        nbinaryButton.setChecked(mSharedPreferences.getBoolean(PROFILE_NB, false));

        // Name
        EditText mNameView = findViewById(R.id.name);
        mNameView.setText(mSharedPreferences.getString(PROFILE_NAME, ""));

        // Email
        EditText mEmailView = findViewById(R.id.email_address);
        mEmailView.setText(mSharedPreferences.getString(PROFILE_EMAIL, ""));
        mEmailView.setEnabled(false);

        // Password
        EditText mPasswordView = findViewById(R.id.password);
        mPasswordView.setText(mSharedPreferences.getString(PROFILE_PASSWORD, ""));

        // Major
        EditText mMajorView = findViewById(R.id.major);
        mMajorView.setText(mSharedPreferences.getString(PROFILE_MAJOR, ""));

        // Phone View
        EditText mPhoneView = findViewById(R.id.phone_number);
        mPhoneView.setText(mSharedPreferences.getString(PROFILE_PHONE, ""));

        // Class Year
        EditText mClassYear = findViewById(R.id.class_year);
        mClassYear.setText(mSharedPreferences.getString(PROFILE_CLASS, ""));
    }
}
