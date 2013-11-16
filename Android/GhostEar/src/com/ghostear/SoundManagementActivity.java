package com.ghostear;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import com.ghostear.R;
import android.app.Activity;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class SoundManagementActivity extends Activity {
	final static int SAMPLING_RATE = 11025;
	AudioRecord audioRec = null;
	Button btn = null;
	boolean bIsRecording = false;
	int bufSize;

	// file pointer
	FileOutputStream out;

	//address and port
	String address;
	int port;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// calcukate buffer size
		bufSize = AudioRecord.getMinBufferSize(
				SAMPLING_RATE,
				AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT) * 2;
		// AudioRecordの作�?
		audioRec = new AudioRecord(
				MediaRecorder.AudioSource.MIC, 
				SAMPLING_RATE,
				AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				bufSize);


		// get parameters
		address = getIntent().getExtras().getString("address");
		port = getIntent().getExtras().getInt("port");
		if(address.length() == 0){
			address = "192.168.2.227";
		}

		if(port == 0){
			port = 12345;
		}

		//connect server
		SoundManagementNative.connectServer(address, port);

		bIsRecording = true;
		startRecoding();
	}

	public void startRecoding(){
		// Start Recoding
		Log.v("AudioRecord", "startRecording");

		// 録音用ファイル取得�? SDカード状態チェ�?��略
		File recFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/rec.raw");
		Log.v("File", Environment.getExternalStorageDirectory().getAbsolutePath() + "/rec.raw");

		try {
			recFile.createNewFile();
			// ファイル書き込み�? 例外�?�?��
			out = new FileOutputStream(recFile);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロ�?��
			e.printStackTrace();
		}

		audioRec.startRecording();

		// Recoding Thread
		new Thread(new Runnable() {
			public void run() {
				byte buf[] = new byte[bufSize];
				while (bIsRecording) {
					// Read recoding data
					audioRec.read(buf, 0, buf.length);							
					Log.v("AudioRecord", "read " + buf.length + " bytes");
					SoundManagementNative.sendSoundData(buf, buf.length);
					//					try {
					//						out.write(buf);
					//					} catch (IOException e) {
					//						// TODO 自動生成された catch ブロ�?��
					//						e.printStackTrace();
					//					}
				}

				try {
					out.close();
				} catch (IOException e) {
					// TODO 自動生成された catch ブロ�?��
					e.printStackTrace();
				}

				//				try {
				//					addWavHeader();
				//					Log.v("####", "addwav");
				//				} catch (IOException e) {
				//					// TODO 自動生成された catch ブロ�?��
				//					e.printStackTrace();
				//				}
			}
		}).start();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.v("AudioRecord", "onDestroy");
		bIsRecording = false;
		
		// Stop Recoding
		Log.v("AudioRecord", "stop");
		audioRec.stop();
		
		// release
		audioRec.release();

		// disconnected
		//SoundManagementNative.closeConnect();

	}



	//WAVヘッ�?をつけて保�?
	//�?��例外�?�?�� チェ�?��処�?��
	public void addWavHeader() throws IOException {
		// 録音したファイル
		File recFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/rec.raw");
		// WAVファイル
		File wavFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/rec.wav");

		wavFile.createNewFile();


		// ストリー�?
		FileInputStream in = new FileInputStream(recFile);
		FileOutputStream outStream = new FileOutputStream(wavFile);

		// ヘッ�?作�?�? サンプルレー�?8kHz
		byte[] header = createHeader(SAMPLING_RATE, (int)recFile.length());
		// ヘッ�?の書き�?�?
		outStream.write(header);

		// 録音したファイルのバイトデータ読み込み
		int n = 0,offset = 0;
		byte[] buffer = new byte[(int)recFile.length()];
		while (offset < buffer.length && (n = in.read(buffer, offset, buffer.length - offset)) >= 0) {
			offset += n;
		}
		// バイトデータ書き込み
		outStream.write(buffer);

		// 終�?
		in.close();
		outStream.close();
	}

	//Wavファイルのヘッ�?を作�?する??PCM16ビッ�? モノラル??
	//sampleRate�? サンプルレー�?
	//datasize �??タサイズ
	//これなんかもっとキレイに書けると思う�? 。�?? Ringroidのソースなんかキレイかも
	public static byte[] createHeader(int sampleRate, int datasize) {
		byte[] byteRIFF = {'R', 'I', 'F', 'F'};
		byte[] byteFilesizeSub8 = intToBytes((datasize + 36));  // ファイルサイズ-8バイト数
		byte[] byteWAVE = {'W', 'A', 'V', 'E'};
		byte[] byteFMT_ = {'f', 'm', 't', ' '};
		byte[] byte16bit = intToBytes(16); // fmtチャンクのバイト数
		byte[] byteSamplerate = intToBytes(sampleRate);  // サンプルレー�?
		byte[] byteBytesPerSec = intToBytes(sampleRate * 2); // バイ�?/�? = サンプルレー�? x 1チャンネル x 2バイ�?
		byte[] bytePcmMono = {0x01, 0x00, 0x01, 0x00}; // フォーマッ�?ID 1 =リニアPCM�? ,�? チャンネル 1 = モノラル
		byte[] byteBlockBit = {0x02, 0x00, 0x10, 0x00}; // ブロ�?��サイズ2バイ�? サンプルあたり�?ビット数16ビッ�?
		byte[] byteDATA = {'d', 'a', 't', 'a'};
		byte[] byteDatasize = intToBytes(datasize);  // �??タサイズ

		ByteArrayOutputStream outArray = new ByteArrayOutputStream();
		try {
			outArray.write(byteRIFF);
			outArray.write(byteFilesizeSub8);
			outArray.write(byteWAVE);
			outArray.write(byteFMT_);
			outArray.write(byte16bit);
			outArray.write(bytePcmMono);
			outArray.write(byteSamplerate);
			outArray.write(byteBytesPerSec);
			outArray.write(byteBlockBit);
			outArray.write(byteDATA);
			outArray.write(byteDatasize);
		} catch (IOException e) {
			return outArray.toByteArray();
		}
		return outArray.toByteArray();
	}
	//int�?32ビットデータをリトルエン�?��アンのバイト�?列にする
	public static byte[] intToBytes(int value) {
		byte[] bt = new byte[4];
		bt[0] = (byte)(value & 0x000000ff);
		bt[1] = (byte)((value & 0x0000ff00) >> 8);
		bt[2] = (byte)((value & 0x00ff0000) >> 16);
		bt[3] = (byte)((value & 0xff000000) >> 24);
		return bt;
	}
}