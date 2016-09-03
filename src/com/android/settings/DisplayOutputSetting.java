/**
 * 
 */
package com.android.settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
/**
 * @author GaoFei
 * DisplayOutput Settings
 *
 */
public class DisplayOutputSetting extends SettingsPreferenceFragment {
	
	public static final String TAG = DisplayOutputSetting.class.getSimpleName();
	/**
	 * display type
	 */
	public static final String DISPLAY_TYPE_DIR = "/sys/class/display";
	/**
	 * root preference screen
	 */
	private PreferenceScreen mRootPreferenceScreen;
	/**
	 * display manager
	 */
	private DisplayManager mDisplayManager;
	/**
	 * mutiltype 
	 */
	private DisplayListener mDisplayListener;
	@Override
	protected int getMetricsCategory() {
		return MetricsLogger.DISPLAY;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "DisplayOutputSetting->getActivity()->class name:" + getActivity().getClass().getName());
		buildPreferenceScreen();
		initData();
	}

	
	@Override
	public void onResume() {
		super.onResume();
		registerDisplayListener();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		unRegisterDisplayListener();
	}
	
	
	public void initData(){
		mDisplayManager = (DisplayManager)getActivity().getSystemService(Context.DISPLAY_SERVICE);
		mDisplayListener = new MultiypeDisplayListener();
	}
	
	/**
	 * 构建Preference列表
	 */
	public void buildPreferenceScreen(){
		if(mRootPreferenceScreen == null)
			mRootPreferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
		else
			mRootPreferenceScreen.removeAll();
		File displayTypeFile = new File(DISPLAY_TYPE_DIR);
		String[] displayTypes = new String[]{};
		if(displayTypeFile != null && displayTypeFile.exists()){
			displayTypes = displayTypeFile.list();
		}
		if(displayTypes != null && displayTypes.length > 0){
			for(String displayType : displayTypes){
				Preference displayPreference = new Preference(getActivity());
				displayPreference.setKey(displayType);
				displayPreference.setTitle(displayType);
				Bundle bundle = displayPreference.getExtras();
				bundle.putString(AllDisplaySetting.EXTRA_BUNDLE_DISPLAY_TYPE, displayType);
				//bundle.putString(key, value)
				displayPreference.setFragment("com.android.settings.AllDisplaySetting");
				mRootPreferenceScreen.addPreference(displayPreference);
			}
		}
		
		
		for(int i = 0; i < mRootPreferenceScreen.getPreferenceCount(); ++i){
			Preference preference = mRootPreferenceScreen.getPreference(i);
			String preferenceKey = preference.getKey();
			File enableFile = new File(DISPLAY_TYPE_DIR + "/" + preferenceKey + "/enable");
			File connectFile = new File(DISPLAY_TYPE_DIR + "/" + preferenceKey + "/connect");
			try{
				BufferedReader enableBufferedReader = new BufferedReader(new FileReader(enableFile));
				BufferedReader connectBufferedReader = new BufferedReader(new FileReader(connectFile));
				String enableStr = enableBufferedReader.readLine();
				String connectStr = connectBufferedReader.readLine();
				if("1".equals(enableStr) && "1".equals(connectStr))
					preference.setEnabled(true);
				else
					preference.setEnabled(false);
				enableBufferedReader.close();
				enableBufferedReader = null;
				connectBufferedReader.close();
				connectBufferedReader = null;
			}catch (Exception e){
				e.printStackTrace();
			}
		
		}
		setPreferenceScreen(mRootPreferenceScreen);
	}
	

	public void registerDisplayListener(){
		mDisplayManager.registerDisplayListener(mDisplayListener, null);
	}
	

	public void unRegisterDisplayListener(){
		mDisplayManager.unregisterDisplayListener(mDisplayListener);
	}
	
	

	class MultiypeDisplayListener implements DisplayListener{

		@Override
		public void onDisplayAdded(int displayId) {
			Log.i(TAG, "onDisplayAdded");
			buildPreferenceScreen();
		}

		@Override
		public void onDisplayRemoved(int displayId) {
			Log.i(TAG, "onDisplayRemoved");
			buildPreferenceScreen();
		}

		@Override
		public void onDisplayChanged(int displayId) {
			
		}
		
	}
}
