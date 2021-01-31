package com.zero.app_installer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/**
 * AppInstallerPlugin
 */
public class AppInstallerPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware , PluginRegistry.ActivityResultListener{

    private Context applicationContext;
    private Activity activity;

    private MethodChannel channel;

    private Result methodResult;

    /**
     * 注册插件
     */
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "app_installer");
        channel.setMethodCallHandler(this);
        this.applicationContext = flutterPluginBinding.getApplicationContext();
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        this.applicationContext = null;
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivity() {
        this.activity = null;
    }


    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 321){
            if (methodResult != null) {
                methodResult.success(data != null);
                methodResult = null;
            }
        }
        return true;
    }


    /**
     * 方法实现
     */
    @Override
    public void onMethodCall(MethodCall call, @NonNull Result result) {
        String method = call.method;
        switch (method) {
            case "goStore":
                String appId = call.argument("androidAppId");
                goAppStore(this.applicationContext, appId);
                result.success(true);
                break;
            case "installApk":
                String filePath = call.argument("apkPath");
                if (!TextUtils.isEmpty(filePath)) {
                    assert filePath != null;
                    installApk(new File(filePath), result);
                } else {
                    result.error("installApk", "apkPath is null", null);
                }
                break;
            case "unInstallApp":
                String packageName = call.argument("packageName");
                if (activity != null) {
                    this.methodResult = result;
                    unInstallApp(this.activity,packageName);
                }
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void unInstallApp(Activity activity, String packageName) {
        Uri uri = Uri.fromParts("package", packageName, null);
        Intent intent = new Intent(Intent.ACTION_DELETE, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        activity.startActivityForResult(intent,321);
    }

    /**
     * 去应用商店
     *
     * @param appId appId
     */
    private void goAppStore(Context context, String appId) {
        String appPackageName;
        // 获取包名
        if (appId != null) {
            appPackageName = appId;
        } else {
            appPackageName = context.getPackageName();
        }
        // 去应用商店
        Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            marketIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        }
        context.startActivity(marketIntent);
    }

    /**
     * 安装Apk
     *
     * @param apkFile 安装文件
     */
    private void installApk(File apkFile, Result result) {
        if (apkFile != null && apkFile.exists() && apkFile.length() > 0) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri contentUri = FileProvider.getUriForFile(applicationContext,
                        applicationContext.getPackageName() + ".fileProvider", apkFile);
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            } else {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            }
            applicationContext.startActivity(intent);
            if (result != null) {
                result.success(true);
            }
        } else {
            if (result != null) {
                result.success(false);
            }
        }
    }

}
