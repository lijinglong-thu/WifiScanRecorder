package com.wifirecorder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class WifiRecorderActivity extends Activity
{
	private static final String PREF_DELAY = "delay";
	private static final String PREF_SCANS = "scans";
	private static final String WIFI_TIMER = "wifi_timer";
	public static final long WIFI_SCAN_DELAY = 5000;
	private static final String TAG = "WifiMapper";
	private static final int SCANS_DEFAULT = 5;//Ĭ��ɨ�����
	private static final int DELAY_DEFAULT = 5;//Ĭ��ɨ����ʱ��

	private Button btnStop;
	private Button btnStart;//��¼wifi��Ѷ
	private Button btnExit;
	private TextView wifiData;//������ʾwifi����
	private TextView scanProgress;//������ʾwifi����
	private TextView viewScanNum;
	private TextView viewDelayNum;
	private EditText editScanNum;
	private EditText eidtDelayNum;
	private WifiManager wifiManager;//��������wifi
	private File wifiRecFile;//���wifi���ݵ��ļ���
	private Timer wifiTimer;
	private WifiBroadcastReceiver wifiScanReciver;
	private ConnectivityManager connectivityManager;
	private int scanCount=0;//ɨ�����
	private EditText edtFileName;//wifi�����ļ���
	private boolean isScan=false;//�Ƿ�ʼ�ɼ���record����ʱ��Ϊtrue
	private int APSum = 20;//Ԥ�ȼƻ���¼��AP����
	private List<ScanResult> firstScan;//��һ��ɨ����

	private int scans = SCANS_DEFAULT;
	private int delay = DELAY_DEFAULT;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "On create");
		setContentView(R.layout.main);
		//��ȡ�ý�����Դ
		btnStop = (Button)findViewById(R.id.btnStop);
		btnStart = (Button)findViewById(R.id.btnStart);
		btnExit = (Button)findViewById(R.id.btnExit);
		wifiData = (TextView)findViewById(R.id.wifiData);
		scanProgress = (TextView)findViewById(R.id.ScanProgress);
		viewScanNum = (TextView)findViewById(R.id.viewScanNum);
		viewDelayNum = (TextView)findViewById(R.id.viewDelayNum);
		editScanNum = (EditText)findViewById(R.id.editScanNum);
		eidtDelayNum = (EditText)findViewById(R.id.editDelayNum);
		//�趨wifiװ��
		wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);//��ȡ��WifiManager
		wifiTimer = new Timer(WIFI_TIMER); //Timer
		wifiScanReciver = new WifiBroadcastReceiver(); //
		connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		//����wifiװ��
		doStartWifi();
		//ȡ��wifi�б��
		//GetWifiList();
		//�趨��ť���ܥ\��
		btnStart.setOnClickListener(btnListener);
		btnStop.setOnClickListener(btnListener);
		btnExit.setOnClickListener(btnListener);

		btnStart.setEnabled(true);
		btnStop.setEnabled(false);
	}

	private Button.OnClickListener btnListener = new Button.OnClickListener()
	{
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch(v.getId())
			{
				case R.id.btnStop:
					//��ȡ��wifi�б��
					doStopScan();
					break;
				case R.id.btnStart:
					RecordCheckWindow();
					break;
				case R.id.btnExit:
					doStopWifi();
					finish();
					break;
			}
		}
	};

	private void RecordCheckWindow()
	{
		Log.d(TAG, "Starting record");
				try {
					doStartScan();
				} catch (Exception e) {
					e.printStackTrace();
				}
	}

	public void CreatFile() throws Exception
	{
		Log.d(TAG, "Create File");
		String dateTime = android.text.format.DateFormat.format("yyyy_MM_dd_hhmm", new java.util.Date()).toString();
		wifiRecFile = new File(getDirectory(),String.format("%s.txt",dateTime));
		//String firstLine = String.format("#%d|%d|%s\n", scans, delay, dateTime);
		StringBuilder firstLine =  new StringBuilder();
		firstLine.append("TimeStamp");
		for(int i=1;i<=APSum;i++){
			firstLine.append(" AP"+i);
		}
		firstLine.append("\n");
		doWriteToFile(wifiRecFile, firstLine.toString());//д���һ�� TimeStamp AP1 AP2 AP3 ����
	}

	public void AddtoFile(List<ScanResult> scanResults) throws Exception {
		Log.d(TAG, "Start addData");
		StringBuilder stringBuilder = new StringBuilder();
		long timestamp = System.currentTimeMillis() / 1000;
		stringBuilder.append(String.format("%s ", timestamp));//��¼ɨ��wifi��ţ�ʱ��
		if (scanCount == 1){
			firstScan = scanResults;
			for (ScanResult firstscanResult : firstScan) {
				stringBuilder.append(String.format("%s|%s ",firstscanResult.BSSID, firstscanResult.level));//��¼wifi��Ϣ
			}
			stringBuilder.append("\n");
			doWriteToFile(wifiRecFile, stringBuilder.toString());
			wifiData.setText(String.format("%s", stringBuilder));
		}
		else {
			//����ѭ���������һ�������������ƥ�䡣
			for (ScanResult firstscanResult : firstScan){
				boolean isCheck = false;//ƥ���Ƿ���ڣ�ƥ�䵽Ϊtrue
				for (ScanResult scanResult : scanResults) {
					if (firstscanResult.BSSID.equals(scanResult.BSSID)){
						stringBuilder.append(String.format("%s|%s ",scanResult.BSSID, scanResult.level));//��¼wifi��Ϣ
						isCheck = true;break;
					}
				}
				if (!isCheck){
					stringBuilder.append(" | ");
				}
			}
			stringBuilder.append("\n");
			doWriteToFile(wifiRecFile, stringBuilder.toString());
			wifiData.setText(String.format("%s", stringBuilder));
		}
	}

	public String getDirectory() {
		return String.format("%s/%s", Environment.getExternalStorageDirectory().toString(), "wifiRec");
	}//��ȡSD��Ŀ¼���ڸ�Ŀ¼���½�һ��wifiRec����Ŀ¼

	//д���ļ�//����ֱ��׷������ĩ
	private void doWriteToFile(File file, String string) throws IOException {
		FileWriter fstream = new FileWriter(file, true);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(string);
		out.close();
	}
	//��wifiװ��
	private void doStartWifi()
	{
		Log.d(TAG, "Starting wifi");
		if (wifiManager.isWifiEnabled()) {
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			Log.d(TAG, "Register wifi reciever");
			registerReceiver(wifiScanReciver, intentFilter);
			wifiManager.startScan();
		} else {
			Log.d(TAG, "Wifi is not enabled");
		}
	}
	//�ر�wifiװ��
	private void doStopWifi()
	{
		Log.d(TAG, "Stopping wifi");
		if (wifiManager.isWifiEnabled()) {
			Log.d(TAG, "Unregister wifi reciever");
			unregisterReceiver(wifiScanReciver);
		}
	}

	private void doStartScan() throws Exception//��ʼɨ��
	{
		Log.d(TAG, "Start scan");
		isScan=true;
		scanCount=0;
		btnStart.setEnabled(false);
		btnStop.setEnabled(true);
		delay = Integer.parseInt(eidtDelayNum.getText().toString());
		scans = Integer.parseInt(editScanNum.getText().toString());
        scanProgress.setText("ScanCount: " + scanCount + "/" + scans);
		CreatFile();
	}

	private void doStopScan()//ɨ��ֹͣ
	{
		Log.d(TAG, "Stop scan");
		if(isScan)
		{
			isScan=false;
			btnStart.setEnabled(true);
			btnStop.setEnabled(false);
		}
	}

	public void doNotify(String message) {
		doNotify(message, false);
	}

	public void doNotify(String message, boolean longMessage) {
		(Toast.makeText(this, message, longMessage ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)).show();
		Log.d(TAG, "Notify: " + message);
	}

	private void GetWifiList(List<ScanResult> scanResults) //��ȡwifiɨ����������
    {
        if(isScan) {
            try {
                scanCount++;
                scanProgress.setText("ScanCount: " + scanCount + "/" + scans);
                AddtoFile(scanResults);
                Log.d(TAG, "Handled wifi scan: #" + scans + ", count: " + this.scanCount + " scans: " + scanResults.size());
				if (scanCount>=scans)
				{
					doStopScan();
				}
			}catch (Exception e){
				Log.e(TAG, e.getMessage(), e);
				doNotify("Error while adding scan to file");
				doStopScan();
			}

		}

	}

	public boolean isWifiConnected() {
		NetworkInfo connectivityNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return connectivityNetworkInfo.isConnected();
	}

	class WifiBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
				GetWifiList(wifiManager.getScanResults());
			if (isWifiConnected()) {
				wifiTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						wifiManager.startScan();
					}
				}, delay * 1000);
			}
		}

	}

}
