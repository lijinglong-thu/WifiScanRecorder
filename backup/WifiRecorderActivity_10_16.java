package com.wifirecorder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
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
	private Button btnRefresh;
	private Button btnRecord;//��¼wifi��Ѷ
	private Button btnExit;
	private TextView txtTime;
	private Calendar time;
	private ListView listWifiResult;//��ʾɨ�赽��wifi��Ϣ
	private List<ScanResult> WifiList;//ɨ�赽��wifi��Ϣ
	private WifiManager mWifiMngr;//��������wifi
	private String[] WifiInfo;//���wifi��ϸ��Ϣ
	private String curTime;
	private Vector<String> WifiSelectedItem = new Vector<String>();//Vector���������飬�����ǿɱ�ģ�������ӡ�ɾ���������
	private File wifiRecFile;//���wifi���ݵ��ļ���
	private StringBuilder stringBuilder;//ÿ�βɼ���wifi����


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		//��ȡ�ý�����Դ
		btnRefresh = (Button)findViewById(R.id.btnRefresh);
		btnRecord = (Button)findViewById(R.id.btnRecord);
		btnExit = (Button)findViewById(R.id.btnExit);
		txtTime = (TextView)findViewById(R.id.txtTime);
		listWifiResult = (ListView)findViewById(R.id.listResult);
		//�趨wifiװ��
		mWifiMngr = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);//��ȡ��WifiManager
		//����wifiװ��
		OpenWifi();
		//ȡ��wifi�б��
		GetWifiList();
		//�趨��ť���ܥ\��
		btnRefresh.setOnClickListener(btnListener);
		btnRecord.setOnClickListener(btnListener);
		btnExit.setOnClickListener(btnListener);
		//�趨listviewѡȡ�¼�
		listWifiResult.setOnItemClickListener(listListener);//�̰�
		listWifiResult.setOnItemLongClickListener(listLongListener);//����
	}

	private Button.OnClickListener btnListener = new Button.OnClickListener()
	{
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch(v.getId())
			{
				case R.id.btnRefresh:
					//��ȡ��wifi�б��
					GetWifiList();
					break;
				case R.id.btnRecord:
					RecordCheckWindow();
					break;
				case R.id.btnExit:
					CloseWifi();
					finish();
					break;
			}
		}
	};

	private ListView.OnItemClickListener listListener = new ListView.OnItemClickListener()
	{
		int ItemSelectedInVector;
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {

			//�������ѡ�ͼ��� Vector
			if(listWifiResult.isItemChecked(position))
				WifiSelectedItem.add(WifiInfo[position]);
			//�p�����ȡ����ѡ�ʹ� Vector �Ƴ�
			else
			{
				//��ȡ��Ŀǰѡȡ��Ŀ��Vector�е�λ��
				for(int i=0;i<WifiSelectedItem.size();i++)
					if(WifiSelectedItem.get(i).equals(WifiInfo[position]))
						ItemSelectedInVector = i;
				WifiSelectedItem.remove(ItemSelectedInVector);
			}
		}

	};
	private ListView.OnItemLongClickListener listLongListener = new ListView.OnItemLongClickListener()
	{

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View v,
				int position, long id) {
			// TODO Auto-generated method stub
			WifiInfo(position);
			return false;
		}
	};
	private void RecordCheckWindow()
	{
		final EditText edtFileName = new EditText(WifiRecorderActivity.this);
		new AlertDialog.Builder(WifiRecorderActivity.this)
		.setTitle("ȷ���Ӵ�")
		.setIcon(R.drawable.ic_launcher)
		.setMessage("������Ԥ�浵������:")
		.setView(edtFileName)
		.setNegativeButton("ȡ��", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub

			}

		})
		.setPositiveButton("ȷ��",new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				//��ѡȡ��list��¼�����ɵ���
				DataFormer(edtFileName.getText().toString());
			}
		}).show();
	}
	private void WifiInfo(int index)
	{
		new AlertDialog.Builder(WifiRecorderActivity.this)
		.setTitle("��ϸ����")
		.setIcon(R.drawable.ic_launcher)
		.setMessage(WifiInfo[index])
		.setNeutralButton("ȷ��", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub

			}

		})
		.show();
	}
	private void DataFormer(String FileName)
	{
		String WifiDatas = curTime+"\r\n";
		File directory = new File(getDirectory());
		//File wifiRecDirectory = new File(getWifiRecDirectory());
		//����������SDcard��
		if(!directory.exists())//�p���SD��û�����Ͼͽ���
			directory.mkdir();
		try {
			//String dateTime = android.text.format.DateFormat.format("yyyy_MM_dd_hhmm", new java.util.Date()).toString();
			wifiRecFile = new File(getDirectory(),String.format("%s.txt",FileName));
			doWriteToFile(wifiRecFile, WifiDatas);
			doWriteToFile(wifiRecFile,stringBuilder.toString());
			Toast.makeText(WifiRecorderActivity.this
							,FileName+".txt �Ѵ����ֻ�",Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Toast.makeText(WifiRecorderActivity.this
							,FileName+"�浵ʧ��!",Toast.LENGTH_LONG).show();
		}
	}

	public String getDirectory() {
		return String.format("%s/%s", Environment.getExternalStorageDirectory().toString(), "wifiRec");
	}//��ȡSD��Ŀ¼���ڸ�Ŀ¼���½�һ��wifimapper����Ŀ¼

	public String getWifiRecDirectory() {
		return String.format("%s/%s", getDirectory(), "mapper");
	}//

	//д���ļ�
	private void doWriteToFile(File file, String string) throws IOException {
		FileWriter fstream = new FileWriter(file, true);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(string);
		out.close();
	}
	//��wifiװ��
	private void OpenWifi()
	{
		//��wifi�ر�ʱ��������
		if(!mWifiMngr.isWifiEnabled()){
			mWifiMngr.setWifiEnabled(true);
			Toast.makeText(WifiRecorderActivity.this,"WiFi�����С������Ժ�"
						   ,Toast.LENGTH_LONG).show();
			Toast.makeText(WifiRecorderActivity.this,"�밴Refresh�������б�"
					,Toast.LENGTH_LONG).show();
		}
	}
	//�ر�wifiװ��
	private void CloseWifi()
	{
		//��ʼɨ��wifi�ȵ�
		if(mWifiMngr.isWifiEnabled())
			mWifiMngr.setWifiEnabled(false);
	}
	private void GetWifiList()
	{
		//��ʼɨ��wifi�ȵ�
		mWifiMngr.startScan();
		//�õ�ɨ����
		WifiList = mWifiMngr.getScanResults();
		//�趨wifi���ЦC
		stringBuilder = new StringBuilder();
		long timestamp = System.currentTimeMillis() / 1000;
		stringBuilder.append(String.format("$%s\n", timestamp));//��¼ɨ��wifi��ţ�ʱ��
		for (ScanResult scanResult : WifiList) {
			stringBuilder.append(String.format("%s %s %s %s\n", scanResult.SSID, scanResult.BSSID, scanResult.level,
					scanResult.frequency));//��¼wifi��Ϣ
		}

		String[] Wifis = new String[WifiList.size()];
		//ȡ��ϵͳ��ǰ���ڣ������զCʱ����
		time = Calendar.getInstance();
		curTime = (time.get(Calendar.YEAR))+"/"
				+(time.get(Calendar.MONTH)+1)+"/"
				+(time.get(Calendar.DAY_OF_MONTH))+"  "
				+time.get(Calendar.HOUR_OF_DAY)+":"
				+time.get(Calendar.MINUTE)+":"
				+time.get(Calendar.SECOND);
		txtTime.setText("Time:"+curTime);
		//��wifi��Ϣ���������У���ѡ�嵥�ã���������ʾ����Ļ�ϵ�list�ϵ�
		for(int i=0;i<WifiList.size();i++)
			Wifis[i] = "SSID:"+WifiList.get(i).SSID +"\n" //SSID
						+"Ѷ��ǿ��:"+WifiList.get(i).level+"dBm";//Ѷ��ǿ��
		//��WifiSelectedItem���ݴ��������ժ�
		WifiSelectedItem.removeAllElements();
		//�趨wifi�嵥
		SetWifiList(Wifis);
	}

	//��¼��ʽ��Ϣ
	public void addScanToFile(List<ScanResult> scanResults) throws Exception
	{
		StringBuilder stringBuilder = new StringBuilder();

		long timestamp = System.currentTimeMillis() / 1000;
		stringBuilder.append(String.format("$%s\n", timestamp));//��¼ɨ��wifi��ţ�ʱ��
		for (ScanResult scanResult : scanResults) {
			stringBuilder.append(String.format("%s %s %s %s\n", scanResult.SSID, scanResult.BSSID, scanResult.level,
					scanResult.frequency));//��¼wifi��Ϣ
		}
		doWriteToFile(wifiRecFile, stringBuilder.toString());
	}
	private void SetWifiList(String[] Wifis)
	{
		//����ArrayAdpter
		 ArrayAdapter<String> adapterWifis = new ArrayAdapter<String>(WifiRecorderActivity.this
						,android.R.layout.simple_list_item_checked,Wifis);
		//�趨ListViewΪ��ѡ
		listWifiResult.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		//�趨ListView��Դ
		listWifiResult.setAdapter(adapterWifis);

		//��ʼ��WifiInfo���ЦC
		WifiInfo = null;
		//��wifi��Ϣ���������У���¼�浵�У�
		WifiInfo = new String[WifiList.size()];

		for(int i=0;i<WifiList.size();i++)
			WifiInfo[i] = "SSID:"+WifiList.get(i).SSID +"\r\n"      //SSID
						+"BSSID:"+WifiList.get(i).BSSID+"\r\n"   //BSSID
						+"Ѷ��ǿ��:"+WifiList.get(i).level+"dBm"+"\r\n" //Ѷ��ǿ��
						+"ͨ��Ƶ��:"+WifiList.get(i).frequency+"MHz"+"\r\n"; //ͨ��Ƶ��
	}

}
