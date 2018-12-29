package io.microshow.rxffmpegdemo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.microshow.rxffmpeg.RxFFmpegInvoke;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    String path = Environment.getExternalStorageDirectory().getPath();

    //需要申请的权限，必须先在AndroidManifest.xml有声明，才可以动态获取权限
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private EditText editText;

    private  ProgressDialog mProgressDialog;

    //权限
    private RxPermissions rxPermissions = null;

    long startTime;//记录开始时间
    long endTime;//记录结束时间

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rxPermissions = new RxPermissions(this);

        editText = (EditText)findViewById(R.id.editText);
        editText.setText("ffmpeg -y -i /storage/emulated/0/1/input.mp4 -vf boxblur=5:1 -preset superfast /storage/emulated/0/1/result.mp4");

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                rxPermissions.request(PERMISSIONS_STORAGE).subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {// 用户同意了权限
//                            runFFmpeg ();
                            runFFmpegRxJava();

                        } else {//用户拒绝了权限
                            Toast.makeText(MainActivity.this,"您拒绝了权限，请往设置里开启权限",Toast.LENGTH_LONG).show();
                        }
                    }
                });

            }
        });

    }

    /**
     * rxjava方式调用
     */
    private void runFFmpegRxJava () {
        openProgressDialog();

        final String text = editText.getText().toString();
        String[] commands = text.split(" ");

        RxFFmpegInvoke.getInstance().runCommandRxJava(commands).subscribe(new Subscriber<Integer>() {

            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Integer progress) {
                if (progress == -100) {//取消状态
                    if (mProgressDialog != null)
                        mProgressDialog.cancel();
                    showDialog("已取消");

                } else {//正在处理 更新进度
                    if (mProgressDialog != null)
                        mProgressDialog.setProgress(progress);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (mProgressDialog != null)
                    mProgressDialog.cancel();
                showDialog("出错了 onError：" + t.getMessage());
            }

            @Override
            public void onComplete() {
                if (mProgressDialog != null)
                    mProgressDialog.cancel();
                showDialog("处理成功");
            }
        });
    }

//    private void runFFmpeg () {
//        openProgressDialog();
//        final String text = editText.getText().toString();
//        String[] commands = text.split(" ");
//
//        FFmpegInvoke.getInstance().runCommandAsync(commands, new FFmpegInvoke.IFFmpegListener() {
//            @Override
//            public void onFinish() {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (mProgressDialog != null)
//                            mProgressDialog.cancel();
//                        showDialog("处理成功");
//                    }
//                });
//            }
//
//            @Override
//            public void onProgress(final int progress) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (mProgressDialog != null)
//                            mProgressDialog.setProgress(progress);
//                    }
//                });
//            }
//
//            @Override
//            public void onCancel() {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (mProgressDialog != null)
//                            mProgressDialog.cancel();
//                        showDialog("已取消");
//                    }
//                });
//            }
//
//            @Override
//            public void onError(final String message) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (mProgressDialog != null)
//                            mProgressDialog.cancel();
//                        showDialog("出错了 onError：" + message);
//                    }
//                });
//            }
//        });
//    }

    public void openProgressDialog() {
        startTime = System.nanoTime();

        mProgressDialog = new ProgressDialog(this);
        final int totalProgressTime = 100;
        mProgressDialog.setMessage("正在转换视频，请稍后...");
        mProgressDialog.setButton("取消", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                RxFFmpegInvoke.getInstance().exit();
            }
        });
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setProgressNumberFormat("");
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMax(totalProgressTime);
        mProgressDialog.show();

    }

    private void showDialog (String message) {
        endTime = System.nanoTime();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示");
        builder.setMessage(message + "\n\n耗时时间：" + TimerUtils.convertUsToTime((endTime-startTime)/1000, false));

        builder.setPositiveButton("关闭", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
//        builder.setCancelable(false);
        builder.create().show();
    }

}