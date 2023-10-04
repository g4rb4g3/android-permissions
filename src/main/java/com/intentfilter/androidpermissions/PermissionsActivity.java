package com.intentfilter.androidpermissions;

import static java.util.Arrays.asList;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.intentfilter.androidpermissions.helpers.Logger;
import com.intentfilter.androidpermissions.models.DeniedPermission;
import com.intentfilter.androidpermissions.models.DeniedPermissions;
import com.intentfilter.androidpermissions.services.BroadcastService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PermissionsActivity extends AppCompatActivity {
    static final int PERMISSIONS_REQUEST_CODE = 100;
    public static final String EXTRA_PERMISSIONS_GRANTED = BuildConfig.LIBRARY_PACKAGE_NAME + ".PERMISSIONS_GRANTED";
    public static final String EXTRA_PERMISSIONS_DENIED = BuildConfig.LIBRARY_PACKAGE_NAME + ".PERMISSIONS_DENIED";
    static final String EXTRA_PERMISSIONS = BuildConfig.LIBRARY_PACKAGE_NAME + ".PERMISSIONS";
    static final Logger logger = Logger.loggerFor(PermissionsActivity.class);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] permissions = getIntent().getStringArrayExtra(EXTRA_PERMISSIONS);
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length == 0 || permissions.length == 0) {
            logger.e("Permission request interrupted. Aborting.");

            PermissionManager.getInstance(this)
                    .removePendingPermissionRequests(asList(getIntent().getStringArrayExtra(EXTRA_PERMISSIONS)));

            //TODO figure out how to finish this activity when request is interrupted to avoid duplicate dialog
            finish();
            return;
        }

        logger.i("RequestPermissionsResult, sending broadcast for permissions " + Arrays.toString(permissions));

        sendPermissionResponse(permissions, grantResults);
        getIntent().putStringArrayListExtra(EXTRA_PERMISSIONS, null);
        finish();
    }

    private void sendPermissionResponse(@NonNull String[] permissions, @NonNull int[] grantResults) {
        Set<String> grantedPermissions = new HashSet<>();
        DeniedPermissions deniedPermissions = new DeniedPermissions();

        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions.add(permissions[i]);
            } else {
                boolean shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i]);
                deniedPermissions.add(new DeniedPermission(permissions[i], shouldShowRationale));
            }
        }

        new BroadcastService(this).broadcastPermissionRequestResult(grantedPermissions, deniedPermissions);
    }

    @Override
    protected void onStop() {
        String[] permissions = getIntent().getStringArrayExtra(EXTRA_PERMISSIONS);

        logger.i("Permission request activity stopped, permission extra: " + (permissions == null ? "null" : "" + permissions.length));

        if (permissions != null && permissions.length > 0) {
            // when the activity is stopped and the array is not empty the user left without allowing/denying anything. assume denied then
            int[] grantResults = new int[permissions.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_DENIED);
            sendPermissionResponse(permissions, grantResults);
        }

        super.onStop();
    }
}