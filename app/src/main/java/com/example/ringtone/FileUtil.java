package com.example.ringtone;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.example.ringtone.MainActivity.TAG;

public class FileUtil {

    public static String getRootPath() {
        String sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory().getAbsolutePath();// 获取跟目录
        }
        if (sdDir != null) {
            return sdDir;
        } else {
            return "";
        }
    }

    public static long getSurplusSpace() {
        return getAvailableSpace(Environment.getExternalStorageDirectory()
                .getAbsolutePath());
    }

    public static long getAvailableSpace(String path) {
        StatFs statFs = new StatFs(path);
        //sd卡可用分区数
        long avCounts = statFs.getAvailableBlocksLong();
        //一个分区数的大小
        long blockSize = statFs.getBlockSizeLong();
        //sd卡可用空间
        long spaceLeft = avCounts * blockSize;
        return spaceLeft;
    }

    public static boolean delete(String path) {
        if (TextUtils.isEmpty(path)) {
            return true;
        }
        File file = new File(path);
        if (!file.exists()) {
            return true;
        }
        return file.delete();
    }

    public static boolean copyFile(String oldPath, String newPath) {
        InputStream inStream = null;
        FileOutputStream fs = null;
        try {
            int byteread = 0;
            File oldfile = new File(oldPath);
            if (!oldfile.exists()) {
                return false;
            }
            File newFile = new File(newPath);
            File parentDir = newFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            if (newFile.exists()) {
                newFile.delete();
            }
            newFile.createNewFile();
            inStream = new FileInputStream(oldPath); //读入原文件
            fs = new FileOutputStream(newPath);
            byte[] buffer = new byte[1444];
            while ((byteread = inStream.read(buffer)) != -1) {
                fs.write(buffer, 0, byteread);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            delete(newPath);
        } finally {
            closeSilence(inStream);
            closeSilence(fs);

        }
        return false;
    }


    public static void closeSilence(Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
//				e.printStackTrace();
            }
        }
    }

    public static boolean rename(String srcFilePath, String targetFilePath) {
        File srcFile = new File(srcFilePath);
        File targetFile = new File(targetFilePath);
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (srcFile.exists()) {
            return srcFile.renameTo(targetFile);
        }
        return false;
    }

    public static String getFileName(String downloadUrl) {
        if (downloadUrl == null) {
            return null;
        }
        if (downloadUrl.contains("/")) {
            String apkName[] = downloadUrl.split("\\/");
            if (apkName.length > 1) {
                return apkName[apkName.length - 1];
            }
        }
        return null;

    }

    /**
     * 判断assets文件夹下的文件是否存在
     *
     * @return false 不存在    true 存在
     */
    public static boolean isFileExists(Context context, String filename, String skinName) {
        AssetManager assetManager = context.getAssets();
        try {
            String[] names = assetManager.list(skinName);
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(filename.trim())) {
                    System.out.println(filename + "存在");
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(filename + "不存在");
            return false;
        }
        System.out.println(filename + "不存在");
        return false;

    }


    /**
     * 从assets目录中复制整个文件夹内容,考贝到 /data/data/包名/files/目录中
     *
     * @param activity activity 使用CopyFiles类的Activity
     * @param filePath String  文件路径,如：/assets/aa
     */
    public static void copyAssetsDir2AppFile(Context activity, String filePath) {
        try {
            String[] fileList = activity.getAssets().list(filePath);
            Log.d("test", "fileList" + fileList.length);
            if (fileList.length > 0) {
                //如果是目录
                File file = new File(getAppFilePathName(filePath, activity));
                boolean mkdirs = file.mkdirs();
                //如果文件夹不存在，则递归
                for (String fileName : fileList) {
                    filePath = filePath + File.separator + fileName;
                    copyAssetsDir2AppFile(activity, filePath);
                    filePath = filePath.substring(0, filePath.lastIndexOf(File.separator));
                    Log.e("test", filePath);
                }
            } else {
                //如果是文件
                copyAssetsFile2AppFile(activity, filePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getAppFilePathName(String filePath, Context context) {
        //外部存储 - 公共目录
//        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + filePath;
        //外部存储 - 私有目录
        return context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES).getAbsolutePath() + File.separator + filePath;
        //内部存储 - 私有目录
//        return context.getFilesDir().getAbsolutePath() + File.separator + filePath;
    }

    /**
     * 将文件从assets目录，考贝到 /data/data/包名/files/ 目录中。assets 目录中的文件，会不经压缩打包至APK包中，使用时还应从apk包中导出来
     *
     * @param fileName 文件名,如aaa.txt
     */
    public static void copyAssetsFile2AppFile(Context activity, String fileName) {
        try {
            InputStream inputStream = activity.getAssets().open(fileName);
            //getFilesDir() 获得当前APP的安装路径 /data/data/包名/files 目录
            File file = new File(getAppFilePathName(fileName, activity));
            FileOutputStream fos = new FileOutputStream(file);//如果文件不存在，FileOutputStream会自动创建文件
            int len = -1;
            byte[] buffer = new byte[1024];
            while ((len = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.flush();//刷新缓存区
            inputStream.close();
            fos.close();
            Log.d(TAG, "copyAssetsFile2AppFile,fileName=" + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}