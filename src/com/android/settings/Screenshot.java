package com.android.settings;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

public class Screenshot extends Application {
	private static Screenshot instance;
    private int mDelayTime;
    private TextView text;
    private WindowManager mWindowManager;
    private Context mContext;
    private Timer timer;
    private WindowManager.LayoutParams params;
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            //TextView text =new TextView(getApplicationContext());
            mDelayTime--;
            Log.d("screenshot", "handleMessage" + "mDelayTime=" + String.valueOf(mDelayTime));
            if (mDelayTime > 1) {
                if (text.getParent() == null) {
                    mWindowManager.addView(text, params);
                } else {
                    text.invalidate();
                }
            } else {
                if (text.getParent() != null) {
                    mWindowManager.removeView(text);
                }
                timer.cancel();
                Intent intent = new Intent();
                intent.setAction("rk.android.screenshot.ACTION");
                mContext.sendBroadcast(intent);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
    	instance = this;
        mContext = this.getApplicationContext();
        text = new MyView(mContext);
        text.setLayoutParams(new LayoutParams(40, 40));
        text.setTextSize(40);
        params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT | WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        params.type = 2006;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.format = PixelFormat.TRANSLUCENT;
        params.gravity = Gravity.LEFT | Gravity.BOTTOM;
        params.width = 50;
        params.height = 40;
        mWindowManager = (WindowManager) mContext.getApplicationContext().getSystemService(mContext.WINDOW_SERVICE);
    }

    public void startScreenshot(int delay) {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        mDelayTime = delay;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                mHandler.sendEmptyMessage(1);
            }
        }, 1000, 1000);
    }

    public class MyView extends TextView {
        public MyView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // TODO Auto-generated method stub
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(30);
            canvas.drawText(String.valueOf(mDelayTime), 0, 40, paint);
            super.onDraw(canvas);
        }
    }
    
    /**
     * 获取当前APP实例
     * @return
     */
    public static Screenshot getAppInstance(){
    	return instance;
    }
    
    
    /**
     * 将值写入至SharedPreference
     * @param key
     * @param value
     */
    public void saveValueToPreference(String key, String value){
    	SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
    	SharedPreferences.Editor editor = sharedPreferences.edit();
    	editor.putString(key, value);
    	editor.commit();
    }
    
    
    /**
     * 从SharedPreference读取值
     * @param key
     * @return
     */
    public String getValueFromPreference(String key){
    	String value = "";
    	value = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE).getString(key, "");
    	return value;
    }
}