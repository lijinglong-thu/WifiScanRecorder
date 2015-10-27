package com.wifirecorder;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
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
	private static final int SCANS_DEFAULT = 10;//Ĭ��ɨ�����
	private static final int DELAY_DEFAULT = 4;//Ĭ��ɨ����ʱ��

	private Button btnStop;
	private Button btnStart;//��¼wifi��Ѷ
	private Button btnExit;
	private Button btnSaveDefault;
	private Button btnReadDefault;
	private TextView wifiData;//������ʾwifi����
	private TextView scanProgress;//������ʾwifi����
	private EditText edtBase;
	private EditText editScanNum;
	private EditText eidtDelayNum;
	private EditText edtLocId;
	private EditText edtSaveFile;
    private ProgressBar scanPb;//ɨ�����

	private WifiManager wifiManager;//��������wifi
	private File wifiRecFile;//���wifi���ݵ��ļ���
	private String baseFile;//��������ļ�������
	private ConnectivityManager connectivityManager;
	private int scanCount=0;//ɨ�����
	private boolean isScan=false;//�Ƿ�ʼ�ɼ���record����ʱ��Ϊtrue
	private int APSum = 50;//Ԥ�ȼƻ���¼��AP����
	private List<ScanResult> firstScan;//��һ��ɨ����
	private List<ScanResult> baseScan;//ɨ�������ܣ���Ϊ�洢�Ļ�׼
    private String LocId;//λ��ID����¼��Wifi��txt��

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
		btnSaveDefault = (Button)findViewById(R.id.btnSaveDefault);
		btnReadDefault = (Button)findViewById(R.id.btnReadDefault);
		wifiData = (TextView)findViewById(R.id.wifiData);
		scanProgress = (TextView)findViewById(R.id.ScanProgress);
		editScanNum = (EditText)findViewById(R.id.editScanNum);
		eidtDelayNum = (EditText)findViewById(R.id.editDelayNum);
		edtBase = (EditText)findViewById(R.id.edtBase);
		edtLocId = (EditText)findViewById(R.id.edtLocId);
		edtSaveFile = (EditText)findViewById(R.id.edtSaveFile);
        scanPb = (ProgressBar)findViewById(R.id.scanPb);
		//�趨wifiװ��
		wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);//��ȡ��WifiManager
		//wifiTimer = new Timer(WIFI_TIMER); //Timer
		//wifiScanReciver = new WifiBroadcastReceiver(); //
		connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		//����wifiװ��
		doStartWifi();

		btnStart.setOnClickListener(btnListener);
		btnStop.setOnClickListener(btnListener);
		btnExit.setOnClickListener(btnListener);
		btnSaveDefault.setOnClickListener(btnListener);
		btnReadDefault.setOnClickListener(btnListener);

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
					Log.d(TAG, "Starting record");
					try {
						doStartScan();
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				case R.id.btnExit:
					doStopWifi();
					finish();
					break;
				case R.id.btnSaveDefault:
                    try {
                        SaveBaseToTxt(baseScan);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
					break;
				case R.id.btnReadDefault:

					break;
			}
		}
	};


	public void CreatFile() throws Exception
	{
		Log.d(TAG, "Create File");
		String dateTime = android.text.format.DateFormat.format("yyyy_MM_dd_hhmm", new java.util.Date()).toString();
		wifiRecFile = new File(getDirectory(),String.format("%s.txt",edtSaveFile.getText().toString()));
		//String firstLine = String.format("#%d|%d|%s\n", scans, delay, dateTime);
		StringBuilder firstLine =  new StringBuilder();
        firstLine.append(dateTime+"\n");
		firstLine.append("TimeStamp ID");
		for(int i=1;i<=APSum;i++){
			firstLine.append(" AP"+i);
		}
		firstLine.append("\n");
		doWriteToFile(wifiRecFile, firstLine.toString());//д���һ�� TimeStamp AP1 AP2 AP3 ����
	}
	//����baseScan��Ϣ��SD���ļ����£������Ժ����
	public void SaveBaseToTxt(List<ScanResult> scanResults) throws Exception{
		File file = new File(getDirectory(),String.format("%s.txt",edtBase.getText().toString()));
        String dateTime = android.text.format.DateFormat.format("yyyy_MM_dd_hhmm", new java.util.Date()).toString();
		StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dateTime+"\n");
		for (ScanResult scanResult : scanResults) {
			stringBuilder.append(String.format("%s %s "+"\n",scanResult.SSID,scanResult.BSSID));//��¼wifi��Ϣ
		}
		doWriteToFile(file,stringBuilder.toString());
        doNotify("Save Successfully!");
	}

	public StringBuilder AddtoFile(List<ScanResult> scanResults) throws Exception {
		Log.d(TAG, "Start addData");
		StringBuilder stringBuilder = new StringBuilder();
		long timestamp = System.currentTimeMillis() / 1000;
		stringBuilder.append(String.format("%s ", timestamp));//��¼ɨ��wifi��ţ�ʱ��
		if (baseScan != null){
			//���baseScan��Ϊnull������baseScan��Ϣ
			for (ScanResult scanResult : scanResults) {
				boolean wasScan = false;
				for (ScanResult baseResult : baseScan){
					if (scanResult.BSSID.equals(baseResult.BSSID)){
						wasScan = true;
						break;
					}
				}
				if (!wasScan){//wasScan Ϊfalse��baseScanδ��⵽
					baseScan.add(scanResult);//����Ӹ�wifi��Ϣ��baseScan
				}
			}
		}
		else {
			baseScan = scanResults;//����baseScanΪnull�򽫸ô�ɨ�踳ֵ��baseScan
		}

			//����ѭ��������baseScan����ƥ�䡣
            stringBuilder.append(LocId+" ");//���ID��Ϣ
			for (ScanResult baseResult : baseScan){
				boolean isCheck = false;//ƥ���Ƿ���ڣ�ƥ�䵽Ϊtrue
				for (ScanResult scanResult : scanResults) {
					if (scanResult.BSSID.equals(baseResult.BSSID)){
						stringBuilder.append(String.format("%s ", scanResult.level));//��¼wifi��Ϣ
						isCheck = true;
                        break;
                    }
                }
                if (!isCheck){
					stringBuilder.append("-100 ");//ĳ��δɨ�赽��,����Ϊ-100
                }
            }
			stringBuilder.append("\n");
			doWriteToFile(wifiRecFile, stringBuilder.toString());
        return stringBuilder;
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
			//IntentFilter intentFilter = new IntentFilter();
            //intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			Log.d(TAG, "Register wifi reciever");
			//registerReceiver(wifiScanReciver, intentFilter);
			wifiManager.startScan();
		} else {
			Log.d(TAG, "Wifi is not enabled");
		}
	}
	//�ر�wifiװ��
	private void doStopWifi() {
        Log.d(TAG, "Stopping wifi");
		if (wifiManager.isWifiEnabled()) {
			Log.d(TAG, "Unregister wifi reciever");
			//unregisterReceiver(wifiScanReciver);
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
        LocId = edtLocId.getText().toString();
        if (baseScan == null){
            CreatFile();//�״����������ļ�
        }
        //�������̶߳�ʱ����
        WifiScanTask wsTask = new WifiScanTask();
        wsTask.execute();
        //new Thread(new scanThread()).start();
	}

	private void doStopScan()//ɨ��ֹͣ
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

	private StringBuilder GetWifiList(List<ScanResult> scanResults) //��ȡwifiɨ����������
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
    //AsyncTask ��
    class WifiScanTask extends AsyncTask<Integer, Integer, String> {
        //����������ڷֱ��ǲ��������������߳���Ϣʱ�䣩������(publishProgress�õ�)������ֵ ����

        @Override
        protected void onPreExecute() {
            //��һ��ִ�з���
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Integer... params) {
            //�ڶ���ִ�з���,onPreExecute()ִ�����ִ��
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
            //���������doInBackground����publishProgressʱ��������Ȼ����ʱֻ��һ������
            //��������ȡ������һ������,����Ҫ��progesss[0]��ȡֵ
            //��n����������progress[n]��ȡֵ
            int value = progress[0];
            scanProgress.setText("ɨ����ȣ� "+value+"/"+scans);
            scanPb.setProgress(value*100/scans);
        }

        @Override
        protected void onPostExecute(String result) {
            //doInBackground����ʱ���������仰˵������doInBackgroundִ����󴥷�
            //�����result��������doInBackgroundִ�к�ķ���ֵ������������"ִ�����"
            wifiData.setText(result);
            doStopScan();
            super.onPostExecute(result);
        }
    }

	@SuppressWarnings("unchecked")
	public static List<ScanResult> String2WifiList(String WifiListString)
			throws StreamCorruptedException, IOException,
			ClassNotFoundException {
		byte[] mobileBytes = Base64.decode(WifiListString.getBytes(),Base64.DEFAULT);
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(mobileBytes);
		ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
		List<ScanResult> WifiList = (List<ScanResult>) objectInputStream.readObject();
		objectInputStream.close();
		return WifiList;
	}
	//SharedPreferences����List����
	public void doDefaultSave(String FileName,List<ScanResult> scanResults){
		Toast.makeText(getApplicationContext(),"save starting!",Toast.LENGTH_LONG).show();
		// ʵ����һ��ByteArrayOutputStream��������װ��ѹ������ֽ��ļ���
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		// Ȼ�󽫵õ����ַ�����װ�ص�ObjectOutputStream
		ObjectOutputStream objectOutputStream = null;
		try{
			objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(scanResults);
			byteArrayOutputStream.close();
			objectOutputStream.close();
		}catch(Exception e){
			try {
				byteArrayOutputStream.close();
				objectOutputStream.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		// writeObject ��������д���ض���Ķ����״̬���Ա���Ӧ�� readObject �������Ի�ԭ��
		String WifiListString = null;
		// �����Base64.encode���ֽ��ļ�ת����Base64���뱣����String��
		try {
			WifiListString = new String(Base64.encode(byteArrayOutputStream.toByteArray(), Base64.DEFAULT));
			Toast.makeText(getApplicationContext(),"save successfully!",Toast.LENGTH_LONG).show();
		}catch (Exception e){
			Toast.makeText(getApplicationContext(),"save failed!",Toast.LENGTH_LONG).show();
		}
			SharedPreferences sharedPreferences= getSharedPreferences(FileName, Context.MODE_PRIVATE);
			SharedPreferences.Editor edit = sharedPreferences.edit();
			edit.putString("wifibase",WifiListString);
			edit.commit();
	}
	@SuppressWarnings("unchecked")
	public List<ScanResult> doDefaultRead(String FileName){
		Toast.makeText(getApplicationContext(),"read starting!",Toast.LENGTH_LONG).show();
		SharedPreferences sharedPreferences = getSharedPreferences(FileName, Context.MODE_PRIVATE);
		String liststr = sharedPreferences.getString("wifibase", "");
		List<ScanResult> wifiList = null;
		//ת���ı�����Ϊ����������
		byte[] mobileBytes = Base64.decode(liststr.getBytes(), Base64.DEFAULT);
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(mobileBytes);
		ObjectInputStream objectInputStream = null;
		try {
			objectInputStream = new ObjectInputStream(byteArrayInputStream);
			wifiList = (List<ScanResult>)objectInputStream.readObject();
			byteArrayInputStream.close();
			objectInputStream.close();
		}catch (Exception e){
			try {
				byteArrayInputStream.close();
				objectInputStream.close();
			}catch (IOException e1){
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return wifiList;
	}

}
