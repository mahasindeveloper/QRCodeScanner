package com.mahasin.qrcodegenerator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import android.view.View;
import java.io.IOException;
import java.io.InputStream;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private SurfaceView cameraPreview;
    private TextView resultTextView;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private final int RequestCameraPermissionID = 1001;
    LinearLayout SelectQR;
    private final int SELECT_IMAGE_REQUEST_CODE = 1002;

    private static final int CAMERA_REQUEST = 50;
    private boolean isFlashlightOn = false;
    private CameraManager cameraManager;
    private String cameraId;
    LinearLayout flashlight_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraPreview = findViewById(R.id.cameraPreview);
        resultTextView = findViewById(R.id.resultTextView);
        SelectQR = findViewById(R.id.SelectQR);

        flashlight_button= findViewById(R.id.flashlight_button);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);


        resultTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the text from resultTextView
                String url = resultTextView.getText().toString();

                // Check if the text is a valid URL
                if (Patterns.WEB_URL.matcher(url).matches()) {
                    // If it's a valid URL, open a WebView
                    Intent webViewIntent = new Intent(MainActivity.this, WebViewActivity.class);
                    webViewIntent.putExtra("url", url); // Pass the URL to the WebViewActivity
                    startActivity(webViewIntent);
                } else {
                    // If it's not a valid URL, display a message or handle accordingly
                    Toast.makeText(MainActivity.this, "Invalid URL", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Inside onCreate method or wherever necessary
        flashlight_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFlashlightOn) {
                    turnOffFlashlight();
                    Toast.makeText(MainActivity.this, "Flashlight turned off", Toast.LENGTH_SHORT).show();
                } else {
                    turnOnFlashlight();
                    Toast.makeText(MainActivity.this, "Flashlight turned on", Toast.LENGTH_SHORT).show();
                }
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
            }
        }

        SelectQR.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, SELECT_IMAGE_REQUEST_CODE);
        });

        barcodeDetector = new BarcodeDetector.Builder(MainActivity.this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        cameraSource = new CameraSource.Builder(MainActivity.this, barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .build();

        cameraPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestCameraPermission();
                } else {
                    startCameraSource();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> qrCodes = detections.getDetectedItems();
                if (qrCodes.size() != 0) {
                    resultTextView.post(() -> {
                        resultTextView.setText(qrCodes.valueAt(0).displayValue);
                    });
                }
            }
        });

    }
    //================================================

    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
            } else {
                // Permission already granted, start the camera
                startCameraSource();
            }
        }
    }

    private void startCameraSource() {
        try {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cameraSource.start(cameraPreview.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri selectedImageUri = data.getData();
                try {
                    InputStream inputStream = MainActivity.this.getContentResolver().openInputStream(selectedImageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    // Attempt to decode the QR code from the selected image
                    decodeQRCodeFromBitmap(bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void decodeQRCodeFromBitmap(Bitmap bitmap) {
        BarcodeDetector detector = new BarcodeDetector.Builder(MainActivity.this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        if (!detector.isOperational()) {
            // Handle detector setup failure
            return;
        }

        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Barcode> barcodes = detector.detect(frame);

        if (barcodes.size() != 0) {
            resultTextView.setText(barcodes.valueAt(0).displayValue);
        } else {
            resultTextView.setText("No QR code found in the selected image.");
        }
    }


    // Modify the turnOnFlashlight method
    private void turnOnFlashlight() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.setTorchMode(cameraId, true);
                isFlashlightOn = true;
            }
        } catch (CameraAccessException | ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to turn on flashlight", Toast.LENGTH_SHORT).show();
            isFlashlightOn = false; // Set flashlight state to off due to failure
        }
    }

    // Modify the turnOffFlashlight method
    private void turnOffFlashlight() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, false);
                isFlashlightOn = false;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to turn off flashlight", Toast.LENGTH_SHORT).show();
        }
    }

            @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start the camera source
                startCameraSource();
            } else {
                // Permission denied, show a message or handle accordingly
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == RequestCameraPermissionID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraSource();
            }
        }
    }

}
