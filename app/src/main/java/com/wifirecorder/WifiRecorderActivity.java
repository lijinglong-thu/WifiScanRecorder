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
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class WifiRecorderActivity extends Activity
{
	private static final String PREF_DELAY = "delay";
	private static final String PREF_SCANS = "scans";
	private static final String WIFI_TIMER = "wifi_timer";
	public static final long WIFI_SCAN_DELAY = 5000;
	private static final String TAG = "WifiMapper";
	private static final int SCANS_DEFAULT = 5;//默认扫描次数
	private static final int DELAY_DEFAULT = 5;//默认扫描间隔时间

	private Button btnStop;
	private Button btnStart;//记录wifi资讯
	private Button btnExit;
	private TextView wifiData;//界面显示wifi数据
	private TextView scanProgress;//界面显示wifi数据
	private TextView viewScanNum;
	private TextView viewDelayNum;
	private EditText editScanNum;
	private EditText eidtDelayNum;
    private ProgressBar scanPb;//扫描进度
	private WifiManager wifiManager;//管理并控制wifi
	private File wifiRecFile;//存放wifi数据的文件名
	private Timer wifiTimer;
	//private WifiBroadcastReceiver wifiScanReciver;
	private ConnectivityManager connectivityManager;
	private int scanCount=0;//扫描次数
	private boolean isScan=false;//是否开始采集，record按下时变为true
	private int APSum = 20;//预先计划记录的AP总数
	private List<ScanResult> firstScan;//第一次扫描结果
    public Thread thScan;

	private int scans = SCANS_DEFAULT;
	private int delay = DELAY_DEFAULT;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "On create");
		setContentView(R.layout.main);
		//取得界面资源
		btnStop = (Button)findViewById(R.id.btnStop);
		btnStart = (Button)findViewById(R.id.btnStart);
		btnExit = (Button)findViewById(R.id.btnExit);
		wifiData = (TextView)findViewById(R.id.wifiData);
		scanProgress = (TextView)findViewById(R.id.ScanProgress);
		viewScanNum = (TextView)findViewById(R.id.viewScanNum);
		viewDelayNum = (TextView)findViewById(R.id.viewDelayNum);
		editScanNum = (EditText)findViewById(R.id.editScanNum);
		eidtDelayNum = (EditText)findViewById(R.id.editDelayNum);
        scanPb = (ProgressBar)findViewById(R.id.scanPb);
		//设定wifi装置
		wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);//取得WifiManager
		wifiTimer = new Timer(WIFI_TIMER); //Timer
		//wifiScanReciver = new WifiBroadcastReceiver(); //
		connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		//启用wifi装置
		doStartWifi();
		//取得wifi列表
		//GetWifiList();
		//设定按钮功能
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
					//取得wifi列表
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
		doWriteToFile(wifiRecFile, firstLine.toString());//写入第一行 TimeStamp AP1 AP2 AP3 ……
	}

	public StringBuilder AddtoFile(List<ScanResult> scanResults) throws Exception {
		Log.d(TAG, "Start addData");
		StringBuilder stringBuilder = new StringBuilder();
		long timestamp = System.currentTimeMillis() / 1000;
		stringBuilder.append(String.format("%s ", timestamp));//记录扫描wifi序号，时间
		if (scanCount == 1){
			firstScan = scanResults;
			for (ScanResult firstscanResult : firstScan) {
                stringBuilder.append(String.format("%s ",firstscanResult.level));//记录wifi信息
            }
			stringBuilder.append("\n");
			doWriteToFile(wifiRecFile, stringBuilder.toString());
			//wifiData.setText(String.format("%s", stringBuilder));
		}
		else {
			//利用循环搜索与第一次搜索结果进行匹配。
			for (ScanResult firstscanResult : firstScan){
				boolean isCheck = false;//匹配是否存在，匹配到为true
				for (ScanResult scanResult : scanResults) {
					if (firstscanResult.BSSID.equals(scanResult.BSSID)){
						stringBuilder.append(String.format("%s ", scanResult.level));//记录wifi信息
						isCheck = true;
                        break;
                    }
                }
                if (!isCheck){
					stringBuilder.append("-110 ");
                }
            }
			stringBuilder.append("\n");
			doWriteToFile(wifiRecFile, stringBuilder.toString());
			//wifiData.setText(String.format("%s", stringBuilder));
		}
        return stringBuilder;
	}

	public String getDirectory() {
		return String.format("%s/%s", Environment.getExternalStorageDirectory().toString(), "wifiRec");
	}//获取SD卡目录，在该目录下新建一个wifiRec的子目录

	//写入文件//可以直接追加在文末
	private void doWriteToFile(File file, String string) throws IOException {
		FileWriter fstream = new FileWriter(file, true);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(string);
		out.close();
	}
	//打开wifi装置
	private void doStartWifi()
	{
		Log.d(TAG, "Starting wifi");
		if (wifiManager.isWifiEnabled()) {
			//IntentFilter intentFilter = new IntentFilter();
            //intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			Log.d(TAG, "Register wifi reciever");
			//registerReceiver(wifiScanReciver, intentFilter);
			wifiManager.startScan();
		} else {
			Log.d(TAG, "Wifi is not enabled");
		}
	}
	//关闭wifi装置
	private void doStopWifi() {
        Log.d(TAG, "Stopping wifi");
		if (wifiManager.isWifiEnabled()) {
			Log.d(TAG, "Unregister wifi reciever");
			//unregisterReceiver(wifiScanReciver);
		}
	}

	private void doStartScan() throws Exception//开始扫描
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
        WifiScanTask wsTask = new WifiScanTask();
        wsTask.execute();
        //new Thread(new scanThread()).start();
	}

	private void doStopScan()//扫描停止
	{
		Log.d(TAG, "Stop scan");
		if(isScan)
		{
			isScan=false;
			btnStart.setEnabled(true);
			btnStop.setEnabled(false);
            Toast.makeText(getApplicationContext(),"scan finished",Toast.LENGTH_LONG).show();
		}
	}


	public void doNotify(String message) {
		doNotify(message, false);
	}

	public void doNotify(String message, boolean longMessage) {
		(Toast.makeText(this, message, longMessage ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)).show();
		Log.d(TAG, "Notify: " + message);
	}

	private StringBuilder GetWifiList(List<ScanResult> scanResults) //获取wifi扫描结果并保存
    {
        StringBuilder stringBuilder = new StringBuilder();
        if(isScan) {
            try {
                scanCount++;
                //scanProgress.setText("ScanCount: " + scanCount + "/" + scans);
                stringBuilder = AddtoFile(scanResults);
                Log.d(TAG, "Handled wifi scans: #" + scans + ", count: " + this.scanCount + " scans: " + scanResults.size());

			}catch (Exception e){
				Log.e(TAG, e.getMessage(), e);
				doNotify("Error while adding scan to file");
				doStopScan();
			}
		}
        return stringBuilder;
	}
    //AsyncTask 类
    class WifiScanTask extends AsyncTask<Integer, Integer, String> {
        //后面尖括号内分别是参数（例子里是线程休息时间），进度(publishProgress用到)，返回值 类型

        @Override
        protected void onPreExecute() {
            //第一个执行方法
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Integer... params) {
            //第二个执行方法,onPreExecute()执行完后执行
           StringBuilder stringBuilder = new StringBuilder();
            for(int i=1;i<=scans;i++){
                wifiManager.startScan();
                stringBuilder.append(GetWifiList(wifiManager.getScanResults())) ;
                publishProgress(i);
                try {
                    Thread.sleep(delay*1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return stringBuilder.toString();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            //这个函数在doInBackground调用publishProgress时触发，虽然调用时只有一个参数
            //但是这里取到的是一个数组,所以要用progesss[0]来取值
            //第n个参数就用progress[n]来取值
            int value = progress[0];
            scanProgress.setText("扫描进度： "+value+"/"+scans);
            scanPb.setProgress(value*100/scans);
        }

        @Override
        protected void onPostExecute(String result) {
            //doInBackground返回时触发，换句话说，就是doInBackground执行完后触发
            //这里的result就是上面doInBackground执行后的返回值，所以这里是"执行完毕"
            wifiData.setText(result);
            doStopScan();
            super.onPostExecute(result);
        }
    }

}
