package com.example.ringtone;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import com.example.ringtone.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    static final String TAG = "Ringtone_custom";
    String appFilePathName;
    ArrayList<String> fileList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setAct(this);
        checkFilePermission();
        getAllRingtones();
    }

    private void cacheRingtoneFile() {
        String assetsFilePath = "ringtone";
        FileUtil.copyAssetsDir2AppFile(this, assetsFilePath);

        appFilePathName = FileUtil.getAppFilePathName(assetsFilePath, this);
        File ringtoneFileDir = new File(appFilePathName);
        Log.d(TAG, "appFilePathName=" + appFilePathName + " ,exists=" + ringtoneFileDir.exists());
        String[] list = ringtoneFileDir.list();
        if (list != null && list.length > 0) {
            for (String s : list) {
                Log.d(TAG, "s=" + s);
                fileList.add(s);
            }
        }

    }

    private void getAllRingtones() {
        final RingtoneManager manager = new RingtoneManager(getApplicationContext());
        manager.setType(RingtoneManager.TYPE_ALL);
        Cursor cursor = manager.getCursor();
        while (!cursor.isAfterLast() && cursor.moveToNext()) {
            int currentPosition = cursor.getPosition();
            Uri ringtongUri = manager.getRingtoneUri(currentPosition);
            String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            Log.d(TAG, "title=" + title + "ringtongUri=" + ringtongUri);
        }
        cursor.close();
    }

    /**
     * WRITE_SETTINGS权限申请
     */
    public void requestWriteSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //开启一个新activity
        startActivityForResult(intent, 110);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean canWrite = Settings.System.canWrite(MainActivity.this);
            Log.d(TAG, "onActivityResult,canWrite=" + canWrite);
            if (Settings.System.canWrite(MainActivity.this)) {
                Log.d(TAG, "onActivityResult,canWrite, continue set ringtone");
            }
        }
    }

    /**
     * 设置来电铃声
     */
    public void setRingtone() {
        if (isNeedRequestWriteSettings() || fileList.isEmpty()) {
            cacheRingtoneFile();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setRingtoneAndroidQ(getRingFilePath(), RingtoneManager.TYPE_RINGTONE);
        } else {
            setRingtone(this, RingtoneManager.TYPE_RINGTONE, getRingFilePath(), getRingFileName());
        }
    }

    /**
     * 设置通知铃声
     */
    public void setMessage() {
        if (isNeedRequestWriteSettings() || fileList.isEmpty()) {
            cacheRingtoneFile();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setRingtoneAndroidQ(getRingFilePath(), RingtoneManager.TYPE_NOTIFICATION);
        } else {
            setRingtone(this, RingtoneManager.TYPE_NOTIFICATION, getRingFilePath(), getRingFileName());
        }
    }

    /**
     * 设置闹钟铃声
     */
    public void setAlarm() {
        if (isNeedRequestWriteSettings() || fileList.isEmpty()) {
            cacheRingtoneFile();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setRingtoneAndroidQ(getRingFilePath(), RingtoneManager.TYPE_ALARM);
        } else {
            setRingtone(this, RingtoneManager.TYPE_ALARM, getRingFilePath(), getRingFileName());
        }
    }

    /**
     * 设置来电，通知和闹钟铃声
     */
    public void setAll() {
        setRingtone();
        setMessage();
        setAlarm();
    }

    public String getRingFilePath() {
        return appFilePathName + File.separator + fileList.get(0);
    }

    public String getRingFileName() {
        return fileList.get(0).replace(".mp3", "");
    }

    public boolean isNeedRequestWriteSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(MainActivity.this)) {
                requestWriteSettings();
                return true;
            }
        }
        return false;
    }


    /**
     * 设置铃声
     * <p>
     * type RingtoneManager.TYPE_RINGTONE 来电铃声
     * RingtoneManager.TYPE_NOTIFICATION 通知铃声
     * RingtoneManager.TYPE_ALARM 闹钟铃声
     * <p>
     * path 下载下来的mp3全路径
     * title 铃声的名字
     */
    public void setRingtone(Context context, int type, String path, String title) {

        Uri oldRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE); //系统当前  电话铃声
        Uri oldNotification = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION); //系统当前  通知铃声
        Uri oldAlarm = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM); //系统当前  闹钟铃声
        Log.d(TAG, "getActualDefaultRingtoneUri, oldRingtoneUri=" + oldRingtoneUri);
        Log.d(TAG, "getActualDefaultRingtoneUri, oldNotification=" + oldNotification);
        Log.d(TAG, "getActualDefaultRingtoneUri, oldAlarm=" + oldAlarm);


        File ringtoneFile = new File(path);
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, ringtoneFile.getAbsolutePath());
        values.put(MediaStore.MediaColumns.TITLE, title);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
        values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
        values.put(MediaStore.Audio.Media.IS_ALARM, true);
        values.put(MediaStore.Audio.Media.IS_MUSIC, true);

        Uri uri = MediaStore.Audio.Media.getContentUriForPath(ringtoneFile.getAbsolutePath());
        Log.d(TAG, "getContentUriForPath, uri=" + uri);
        Uri newUri = null;
        String deleteId = "";
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, MediaStore.MediaColumns.DATA + "=?", new String[]{path}, null);
            if (cursor.moveToFirst()) {
                deleteId = cursor.getString(cursor.getColumnIndex("_id"));
            }
            //LogTool.e("AGameRing", "deleteId:" + deleteId);
            context.getContentResolver().delete(uri, MediaStore.MediaColumns.DATA + "=\"" + ringtoneFile.getAbsolutePath() + "\"", null);
            newUri = context.getContentResolver().insert(uri, values);
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (newUri != null) {
            String uriPath = getFilePathFromContentUri(newUri, getContentResolver());
            Log.d(TAG, "setRingtone uriPath: " + uriPath + " ,newUri=" + newUri);

            String ringtoneId = "";
            String notificationId = "";
            String alarmId = "";
            if (null != oldRingtoneUri) {
                ringtoneId = oldRingtoneUri.getLastPathSegment();
            }

            if (null != oldNotification) {
                notificationId = oldNotification.getLastPathSegment();
            }

            if (null != oldAlarm) {
                alarmId = oldAlarm.getLastPathSegment();
            }

            Uri setRingStoneUri;
            Uri setNotificationUri;
            Uri setAlarmUri;

            if (type == RingtoneManager.TYPE_RINGTONE || ringtoneId.equals(deleteId)) {
                setRingStoneUri = newUri;
            } else {
                setRingStoneUri = oldRingtoneUri;
            }

            if (type == RingtoneManager.TYPE_NOTIFICATION || notificationId.equals(deleteId)) {
                setNotificationUri = newUri;
            } else {
                setNotificationUri = oldNotification;
            }

            if (type == RingtoneManager.TYPE_ALARM || alarmId.equals(deleteId)) {
                setAlarmUri = newUri;
            } else {
                setAlarmUri = oldAlarm;
            }

            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, setRingStoneUri);
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION, setNotificationUri);
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM, setAlarmUri);

            switch (type) {
                case RingtoneManager.TYPE_RINGTONE:
                    Toast.makeText(context.getApplicationContext(), "设置来电铃声成功！", Toast.LENGTH_SHORT).show();
                    break;
                case RingtoneManager.TYPE_NOTIFICATION:
                    Toast.makeText(context.getApplicationContext(), "设置通知铃声成功！", Toast.LENGTH_SHORT).show();
                    break;
                case RingtoneManager.TYPE_ALARM:
                    Toast.makeText(context.getApplicationContext(), "设置闹钟铃声成功！", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }


    private void checkFilePermission() {
        String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, perms, 123);
        } else {
            cacheRingtoneFile();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult permissions = " + Arrays.toString(permissions) + "  grantResults = " + Arrays.toString(grantResults));
        if (verifyPermissions(grantResults)) {
            cacheRingtoneFile();
        }
    }

    /**
     * Checks all given permissions have been granted.
     *
     * @param grantResults results
     * @return returns true if all permissions have been granted.
     */
    public static boolean verifyPermissions(int... grantResults) {
        if (grantResults.length == 0) {
            return false;
        }
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    /**
     * 适配android q以上版本  android 9
     *
     * @param ringtoneFilePath ringtoneFilePath
     * @param type             type
     */
    private void setRingtoneAndroidQ(String ringtoneFilePath, int type) {
        File ringtoneFile = new File(ringtoneFilePath);
        Log.d(TAG, "---setRingtoneAndroidQ---" + ringtoneFile.getPath());
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, getRingFileName());
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/x-mpeg");
        values.put(MediaStore.Audio.Media.TITLE, getRingFileName());
        values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/test");
        values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
        values.put(MediaStore.Audio.Media.IS_ALARM, true);
        values.put(MediaStore.Audio.Media.IS_MUSIC, true);

        Uri external = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;//MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver resolver = getContentResolver();

        Uri insertUri = resolver.insert(external, values);

        Log.d(TAG, "insertUri: " + insertUri);

        String uriPath = getFilePathFromContentUri(insertUri, getContentResolver());
        Log.d(TAG, "setRingtoneAndroidQ uriPath: " + uriPath);


        OutputStream os = null;
        FileInputStream inputStream = null;

        if (insertUri != null) {
            try {
                os = resolver.openOutputStream(insertUri);
                if (os != null) {
                    inputStream = new FileInputStream(ringtoneFile);
                    byte[] bytes = new byte[1024];
                    int len = 0;
                    while ((len = inputStream.read(bytes)) != -1) {
                        os.write(bytes, 0, len);
                    }
                    inputStream.close();
                    os.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        RingtoneManager.setActualDefaultRingtoneUri(this, type, insertUri);
        Log.d(TAG, "insertUri: " + insertUri);
        Toast.makeText(this, "铃声设置完成", Toast.LENGTH_SHORT).show();
    }

    private String getFilePathFromContentUri(Uri selectedUri, ContentResolver contentResolver) {
        String filePath;

        String[] filePathColumn = {MediaStore.MediaColumns.DATA};

        Cursor cursor = contentResolver.query(selectedUri, filePathColumn, null, null, null);

        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);

        filePath = cursor.getString(columnIndex);

        cursor.close();

        return filePath;

    }
}