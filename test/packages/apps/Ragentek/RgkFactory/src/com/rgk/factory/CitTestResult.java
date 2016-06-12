package com.rgk.factory;

import com.rgk.factory.ControlCenter.ResultHandle;
import com.rgk.factory.backled.BackLED;
import com.rgk.factory.battery.Battery;
import com.rgk.factory.bluetooth.Bluetooth;
import com.rgk.factory.sensor.Gyroscope;
import com.rgk.factory.sensor.MSensor;
import com.rgk.factory.sensor.SensorCalibration;
import com.rgk.factory.sensor.DistanceSensor;
import com.rgk.factory.earphone.EarPhone;
import com.rgk.factory.fingerprint.FingerPrint;
import com.rgk.factory.fm.FM;
import com.rgk.factory.gps.GPS;
import com.rgk.factory.sensor.GravitySensor;
import com.rgk.factory.hall.Hall;
import com.rgk.factory.headset.HeadSet;
import com.rgk.factory.key.KeyTestActivity;
import com.rgk.factory.lcd.LCD;
import com.rgk.factory.lcdblur.LcdBlur;
import com.rgk.factory.sensor.LightSensor;
import com.rgk.factory.loundspeaker.LoundSpeaker;
import com.rgk.factory.maincamera.MainCamera;
import com.rgk.factory.memory.Memory;
import com.rgk.factory.microphone.MicrPhone;
import com.rgk.factory.microphone2.MicrPhone2;
import com.rgk.factory.notificationlight.NotificationLight;
import com.rgk.factory.otg.Otg;
import com.rgk.factory.sdcard.SdCard;
import com.rgk.factory.simcard.SimCard;
import com.rgk.factory.simsignal.SimSignal;
import com.rgk.factory.subcamera.SubCamera;
import com.rgk.factory.telephony.Telephony;
import com.rgk.factory.temperature.Temperature;
import com.rgk.factory.touchpanel.TouchPanel;
import com.rgk.factory.versioninfo.MotherboardInfo;
import com.rgk.factory.versioninfo.VersionInfo;
import com.rgk.factory.vibrator.Vibration;
import com.rgk.factory.wifi.WiFi;
import com.rgk.factory.tdcoder.CaptureActivity;
import com.rgk.factory.flash.FlashlightManager;
import com.rgk.factory.flash.ProactiveFlashlightManager;
import com.rgk.factory.buttonlight.ButtonLightActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CitTestResult extends Activity {
	private TextView testTitle,testResult,testTitle_exclude,testResult_exclude;
	public static String TAG = "CitTestResult";
	LinearLayout progress;
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "action="+intent.getAction());
			if(intent.getAction().equals(Config.ACTION_AUTO_BACKTEST)) {
				progress.setVisibility(View.GONE);
				initText();
			}
		}
    };
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
              // M: huangkunming 2014.12.20 @{
              // since window flags full, change FLAG_HOMEKEY_DISPATCHED to privateFlags
        	//getWindow().addFlags(WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED);
        	getWindow().getAttributes().privateFlags |= WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED;
    	       // @}
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		super.onCreate(savedInstanceState);
		setTitle(R.string.test_results);
		setContentView(R.layout.test_results);
		LinearLayout mLayout = (LinearLayout) findViewById(R.id.test_results_layout);
	    mLayout.setSystemUiVisibility(0x00002000);
	    progress = (LinearLayout) findViewById(R.id.progress);
	    //if(!MainActivity.mAutoBackTest) {
	    	progress.setVisibility(View.GONE);
	    	initText();
	    //}
	    IntentFilter filter = new IntentFilter(Config.ACTION_AUTO_BACKTEST);
	    registerReceiver(mReceiver, filter);
	}
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(mReceiver);
		super.onDestroy();
	}

	public void initText() {
		testTitle = (TextView) findViewById(R.id.testTitle);
		testResult = (TextView) findViewById(R.id.testResult);
		testTitle_exclude = (TextView) findViewById(R.id.testTitle_exclude);
		testResult_exclude = (TextView) findViewById(R.id.testResult_exclude);
		
		StringBuilder title = new StringBuilder();
		StringBuilder result = new StringBuilder();
		
		StringBuilder title_exclude = new StringBuilder();
		StringBuilder result_exclude = new StringBuilder();
		//version info
		title.append(getResources().getString(R.string.ver_info)+"\n");
		result.append(getText(VersionInfo.TAG)+"\n");
		//motherboard info
		title.append(getResources().getString(R.string.board_info)+"\n");
		result.append(getText(MotherboardInfo.TAG)+"\n");

		if(Util.backLed) {
			title.append(getResources().getString(R.string.BackLED)+"\n");
			result.append(getText(BackLED.TAG)+"\n");
		}
		if(Util.lcd) {
			title.append(getResources().getString(R.string.lcd)+"\n");
			result.append(getText(LCD.TAG)+"\n");
		}	
		if(Util.touchPanel) {
			title.append(getResources().getString(R.string.touch_panel)+"\n");
			result.append(getText(TouchPanel.TAG)+"\n");			
		}
		if(Util.key) {
			title.append(getResources().getString(R.string.key)+"\n");
			result.append(getText(KeyTestActivity.TAG)+"\n");
		}
		if(Util.vibrator) {
			title.append(getResources().getString(R.string.Vibrator)+"\n");
			result.append(getText(Vibration.TAG)+"\n");
		}
		if(Util.gravitySensor) {
			title.append(getResources().getString(R.string.Gravity_Sensor)+"\n");
			result.append(getText(GravitySensor.TAG)+"\n");
		}
		if(Util.calibration) {
			title.append(getResources().getString(R.string.Calibration)+"\n");
			result.append(getText(SensorCalibration.TAG)+"\n");
		}
		if(Util.distanceSensor) {
			title.append(getResources().getString(R.string.Distance_Sensor)+"\n");
			result.append(getText(DistanceSensor.TAG)+"\n");
		}
		if(Util.lightSensor) {
			title.append(getResources().getString(R.string.Light_Sensor)+"\n");
			result.append(getText(LightSensor.TAG)+"\n");
		}
		if(Util.magnetic) {
			title.append(getResources().getString(R.string.magnetic)+"\n");
			result.append(getText(MSensor.TAG)+"\n");
		}
		if(Util.gyroscope) {
			title.append(getResources().getString(R.string.gyroscope)+"\n");
			result.append(getText(Gyroscope.TAG)+"\n");
		}
		if(Util.hall) {
			title.append(getResources().getString(R.string.hall)+"\n");
			result.append(getText(Hall.TAG)+"\n");
		}		
		if(Util.loundSpeaker) {
			title.append(getResources().getString(R.string.LoundSpeaker)+"\n");
			result.append(getText(LoundSpeaker.TAG)+"\n");
		}
		if(Util.mainCamera) {
			title.append(getResources().getString(R.string.Main_Camera)+"\n");
			result.append(getText(MainCamera.TAG)+"\n");
		}
		if(Util.subCamera) {
			title.append(getResources().getString(R.string.sub_Camera)+"\n");
			result.append(getText(SubCamera.TAG)+"\n");
		}
		if(Util.flash) {
			title.append(getResources().getString(R.string.key_Flash)+"\n");
			result.append(getText(FlashlightManager.TAG)+"\n");
		}
		if(Util.proactiveFlash) {
			title.append(getResources().getString(R.string.proactive_flash)+"\n");
			result.append(getText(ProactiveFlashlightManager.TAG)+"\n");
		}		
		if(Util.earphone) {
			title.append(getResources().getString(R.string.Earphone)+"\n");
			result.append(getText(EarPhone.TAG)+"\n");
		}
		if(Util.fm) {
			title.append(getResources().getString(R.string.FM)+"\n");
			result.append(getText(FM.TAG)+"\n");
		}
		if(Util.battery) {
			title.append(getResources().getString(R.string.battery)+"\n");
			result.append(getText(Battery.TAG)+"\n");
		}
		if(Util.telephony) {
			title.append(getResources().getString(R.string.Telephony)+"\n");
			result.append(getText(Telephony.TAG)+"\n");
		}
		if(Util.buttonLight) {
			title.append(getResources().getString(R.string.button_light)+"\n");
			result.append(getText(ButtonLightActivity.TAG)+"\n");
		}
		if(Util.notificationLight) {
			title.append(getResources().getString(R.string.notification_light)+"\n");
			result.append(getText(NotificationLight.TAG)+"\n");
		}
		if(Util.otg) {
			title.append(getResources().getString(R.string.otg)+"\n");
			result.append(getText(Otg.TAG)+"\n");
		}		
		if(Util.memory) {
			title.append(getResources().getString(R.string.Memory)+"\n");
			result.append(getText(Memory.TAG)+"\n");
		}
		if(Util.sdCard) {
			title.append(getResources().getString(R.string.SDcard)+"\n");
			result.append(getText(SdCard.TAG)+"\n");
		}	
		if(Util.simCard) {
			title.append(getResources().getString(R.string.SIM_Card)+"\n");
			result.append(getText(SimCard.TAG)+"\n");
		}
		if(Util.gps) {
			title.append(getResources().getString(R.string.gps)+"\n");
			result.append(getText(GPS.TAG)+"\n");
		}
		if(Util.bluetooth) {
			title.append(getResources().getString(R.string.Bluetooth)+"\n");
			result.append(getText(Bluetooth.TAG)+"\n");
		}
		if(Util.wiFi) {
			title.append(getResources().getString(R.string.WiFi)+"\n");
			result.append(getText(WiFi.TAG)+"\n");
		}
		if(Util.microphone) {
			title.append(getResources().getString(R.string.Microphone)+"\n");
			result.append(getText(MicrPhone.TAG)+"\n");
		}
		if(Util.microphone2) {
			title.append(getResources().getString(R.string.Microphone2)+"\n");
			result.append(getText(MicrPhone2.TAG)+"\n");
		}
		if(Util.tdCode) {
			title.append(getResources().getString(R.string.TD_code)+"\n");
			result.append(getText(CaptureActivity.TAG)+"\n");
		}
		if(Util.simSignal) {
			title.append(getResources().getString(R.string.SIM_Signal)+"\n");
			result.append(getText(SimSignal.TAG)+"\n");
		}
		if(Util.temperature) {
			title.append(getResources().getString(R.string.temperature)+"\n");
			result.append(getText(Temperature.TAG)+"\n");
		}
		if(Util.fingerprint) {
			title.append(getResources().getString(R.string.FingerPrint)+"\n");
			result.append(getText(FingerPrint.TAG)+"\n");
		}		
		//Exclude the following test items
		if(Util.headset) {
			title_exclude.append(getResources().getString(R.string.Headset)+"\n");
			result_exclude.append(getText(HeadSet.TAG)+"\n");
		}
		if(Util.lcdBlur) {
			title_exclude.append(getResources().getString(R.string.lcd_blur)+"\n");
			result_exclude.append(getText(LcdBlur.TAG)+"\n");
		}
		
		testTitle.setText(title);
		testResult.setText(result);
		
		testTitle_exclude.setText(title_exclude);
		testResult_exclude.setText(result_exclude);
	}
	
	private String getText(String tag) {
		// TODO Auto-generated method stub
		return ResultHandle.getTextFromSystem(tag);
	}
}