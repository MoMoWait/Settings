
package com.android.settings;
import com.android.internal.logging.MetricsLogger;
import android.util.Log;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import javax.xml.transform.Result;
import com.android.settings.widget.SwitchBar;
import android.os.AsyncTask;
import android.os.SystemProperties;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Toast;
//import static android.provider.Settings.System.HDMI_LCD_TIMEOUT;
import android.content.ContentResolver;
import android.os.Handler;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.os.RemoteException;
import android.os.DisplayOutputManager;


/**
 * @author GaoFei
 * 所有显示输出设置的入口
 */
public class AllDisplaySetting extends SettingsPreferenceFragment
		implements OnPreferenceChangeListener{
	private static final String TAG = "AllDisplaySetting";
	private static final String KEY_HDMI_RESOLUTION = "hdmi_resolution";
	private static final String KEY_HDMI_LCD = "hdmi_lcd_timeout";
	private static final String KEY_HDMI_SCALE="hdmi_screen_zoom";
	// for identify the HdmiFile state
	private boolean IsHdmiConnect = false;
	// for identify the Hdmi connection state
	private boolean IsHdmiPlug = false;
	private boolean IsHdmiDisplayOn = false;

	private ListPreference mHdmiResolution;
	private ListPreference mHdmiLcd;
	private Preference mHdmiScale;
	private File HdmiDisplayModes=null;
	private Context context;
	private static final int DEF_HDMI_LCD_TIMEOUT_VALUE = 10;
	private static final String SET_MODE="set_mode";
	private static final String GET_MODE="get_mode";
	private static final String HDMI_CONNECTED="connect";
	private static final String HDMI_DISCONNECTED="disconnect";
	/**
	 * 显示类型 HDMI,DP,VGA等
	 */
	public static final String EXTRA_BUNDLE_DISPLAY_TYPE = "extra_bundle_display_type";
	/**
	 * 显示配置的目录
	 */
	public static final String DISPLAY_DIR = "sys/class/display";
	/**
	 * mode文件
	 */
	public static final String MODE = "mode";
	/**
	 * modes文件
	 */
	public static final String MODES = "modes";
	/**
	 * connect文件
	 */
	public static final String CONNECT = "connect";
	/**
	 * enable文件
	 */
	public static final String ENABLE = "enable";
	
	/**
	 * 显示类型
	 */
	private String mDisplayType;
	/**
	 * 显示管理
	 */
	private DisplayManager mDisplayManager;
	private DisplayChangeListener mChangeListener;
	@Override
	protected int getMetricsCategory() {
	    return MetricsLogger.DISPLAY;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getActivity();
		Log.i(TAG, "HdmiSettings->getActivity():" + context.getClass().getName());
		addPreferencesFromResource(R.xml.hdmi_settings_timeout);
		initData();
		initView();
		refreshViewAndData();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d(TAG,"onCreateView----------------------------------------");
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
		registerDisplayListener();
		getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.HDMI_LCD_TIMEOUT),true, mHdmiTimeoutSettingObserver);
	}


	@Override
	public void onPause() {
		super.onPause();
		unRegisterDisplayListener();
		Log.d(TAG,"onPause----------------");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getContentResolver().unregisterContentObserver(
				mHdmiTimeoutSettingObserver);
	}

	
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		return true;
	}

	
	@Override
	public boolean onPreferenceChange(Preference preference, Object objValue) {
		// TODO Auto-generated method stub
		String key = preference.getKey();
		Log.d(TAG, key);
		Log.i(TAG, "onPreferenceChange->objValue:" + objValue);
		if (KEY_HDMI_RESOLUTION.equals(key)) {
			updateMode(objValue.toString() + "\n");
		}

		if (KEY_HDMI_LCD.equals(key)) {
			try {
				String strMode = "hdmi_display";
				int value = Integer.parseInt((String) objValue);
				// editor.putInt("enable", value);
				setHdmiLcdTimeout(value);
			} catch (NumberFormatException e) {
				Log.e(TAG, "onPreferenceChanged hdmi_mode setting error");
			}
		}
		//editor.commit();
		return true;
	}
	
	
	
	/**
	 * LCD time out改变监听器
	 */
	private ContentObserver mHdmiTimeoutSettingObserver = new ContentObserver(
			new Handler()) {
		@Override
		public void onChange(boolean selfChange) {

			ContentResolver resolver = getActivity().getContentResolver();
			final long currentTimeout = Settings.System.getLong(resolver,
					Settings.System.HDMI_LCD_TIMEOUT, -1);
			long lcdTimeout = -1;
			if ((lcdTimeout = Settings.System.getLong(resolver,
					Settings.System.HDMI_LCD_TIMEOUT,
					DEF_HDMI_LCD_TIMEOUT_VALUE)) > 0) {
				lcdTimeout /= 10;
			}
			mHdmiLcd.setValue(String.valueOf(lcdTimeout));
		}
	};

	private void initLcdTimeOut() {
		//int dualMode=sharedPreferences.getInt("dual_mode", 0);
		int dualMode = 0;
		if(dualMode==0){
			AllDisplaySetting.this.getPreferenceScreen().removePreference(mHdmiLcd);
		}else{
			mHdmiLcd.setOnPreferenceChangeListener(AllDisplaySetting.this);
			ContentResolver resolver = context.getContentResolver();
			long lcdTimeout = -1;
			if ((lcdTimeout = Settings.System.getLong(resolver,
					Settings.System.HDMI_LCD_TIMEOUT,
					DEF_HDMI_LCD_TIMEOUT_VALUE)) > 0) {
				lcdTimeout /= 10;
			}
			//String enable = sharedPreferences.getString("enable", "1");
			String enable = "1";
			mHdmiLcd.setValue(String.valueOf(lcdTimeout));
			mHdmiLcd.setEnabled(enable.equals("1"));
		}
	}
	


	private void setHdmiLcdTimeout(int value) {
		if (value != -1) {
			value = (value) * 10;
		}
		HdmiTimeoutSettingTask task=new HdmiTimeoutSettingTask();
		task.execute(Integer.valueOf(value));
		
		
	}
	

	/**
	 * 获取屏幕分辨率，不只是针对HDMI
	 * @return
	 */
	private String[] getModes() {
		ArrayList<String> list = new ArrayList<String>();
		try {
			FileReader fread = new FileReader(new File(DISPLAY_DIR + "/" + mDisplayType + "/" + MODES));
			BufferedReader buffer = new BufferedReader(fread);
			String str = null;

			while ((str = buffer.readLine()) != null) {
				list.add(str);
			}
			fread.close();
			buffer.close();
		} catch (IOException e) {
			Log.e(TAG, "IO Exception");
		}
		return list.toArray(new String[list.size()]);
	}
	
	
	
	private class HdmiTimeoutSettingTask extends AsyncTask<Integer, Void, Void>{

		@Override
		protected Void doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			try {
				Settings.System.putInt(getContentResolver(),
						Settings.System.HDMI_LCD_TIMEOUT, params[0]);
			} catch (NumberFormatException e) {
				Log.e(TAG, "could not persist hdmi lcd timeout setting", e);
			}
			return null;
		}
		
	}
		
	
	/**
	 * 初始化数据，获取控件引用等
	 */
	public void initData(){
		mDisplayManager = (DisplayManager)getActivity().getSystemService(Context.DISPLAY_SERVICE);
		mChangeListener = new DisplayChangeListener();
		mHdmiLcd = (ListPreference) findPreference(KEY_HDMI_LCD);
		mHdmiResolution = (ListPreference) findPreference(KEY_HDMI_RESOLUTION);
		mHdmiResolution.setOnPreferenceChangeListener(this);
		mHdmiScale = findPreference(KEY_HDMI_SCALE);
		Log.d(TAG,"onCreate---------------------");
		//获取显示类型
		mDisplayType = getArguments().getString(AllDisplaySetting.EXTRA_BUNDLE_DISPLAY_TYPE);
		//设置Resolution Title
		mHdmiResolution.setTitle(mDisplayType + getString(R.string.resolution));
		Log.i(TAG, "initData->mDisplayType:" + mDisplayType);
		//initDualMode();
		//HdmiIsConnectTask task=new HdmiIsConnectTask();
		//task.execute();
	}
	
	
	public void initView(){
		//移除lcd_time_out
		getPreferenceScreen().removePreference(mHdmiLcd);
	}
	
	
	/**
	 * 刷新UI与数据
	 */
	public void refreshViewAndData(){
		File enableFile = new File(DISPLAY_DIR + "/" + mDisplayType + "/" + ENABLE);
		File connectFile = new File(DISPLAY_DIR + "/" + mDisplayType + "/" +  CONNECT);
		try{
			BufferedReader enableBufferedReader = new BufferedReader(new FileReader(enableFile));
			BufferedReader connectBufferedReader = new BufferedReader(new FileReader(connectFile));
			String enableStr = enableBufferedReader.readLine();
			String connectStr = connectBufferedReader.readLine();
			if("1".equals(enableStr) && "1".equals(connectStr)){
				mHdmiResolution.setEnabled(true);
				mHdmiScale.setEnabled(true);
				mHdmiLcd.setEnabled(true);
			}else{
				mHdmiResolution.setEnabled(false);
				mHdmiScale.setEnabled(false);
				mHdmiLcd.setEnabled(false);
			}
			enableBufferedReader.close();
			enableBufferedReader = null;
			connectBufferedReader.close();
			connectBufferedReader = null;
		}catch (Exception e){
			e.printStackTrace();
		}
		
		String[] modes = getModes();
		if(modes != null && modes.length > 0){
			mHdmiResolution.setEntries(modes);
			mHdmiResolution.setEntryValues(modes);
		}
	}
	
	
	/**
	 * 更新显示分辨率
	 */
	public void updateMode(String mode){
		try{
			FileWriter modeWriter = new FileWriter(new File(DISPLAY_DIR + "/" + mDisplayType + "/" + MODE));
			modeWriter.write(mode);
			modeWriter.flush();
			modeWriter.close();
			modeWriter = null;
		}catch (Exception e){
			Log.i(TAG, "updateMode->Exception:" + e);
		}
	}
	
	/**
	 * 注册显示改变监听器
	 */
	public void registerDisplayListener(){
		mDisplayManager.registerDisplayListener(mChangeListener, null);
	}
	
	/**
	 * 取消注册显示改变
	 */
	public void unRegisterDisplayListener(){
		mDisplayManager.unregisterDisplayListener(mChangeListener);
	}
	
	
	/**
	 * 
	 * @author GaoFei
	 * 显示改变监听器
	 */
	class DisplayChangeListener implements DisplayListener{

		@Override
		public void onDisplayAdded(int displayId) {
			refreshViewAndData();
		}

		@Override
		public void onDisplayRemoved(int displayId) {
			refreshViewAndData();
		}

		@Override
		public void onDisplayChanged(int displayId) {
			
		}
		
	}
}
