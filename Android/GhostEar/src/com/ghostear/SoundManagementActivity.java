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
import java.nio.ByteBuffer;
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
	final static int SAMPLING_RATE = 16000;
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

		// Calculation of the buffer size
		bufSize = AudioRecord.getMinBufferSize(
				SAMPLING_RATE,
				AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT) * 2;
		// Creating AudioRecord
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

		// get file path
		File recFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/rec.raw");
		Log.v("File", Environment.getExternalStorageDirectory().getAbsolutePath() + "/rec.raw");

		try {
			recFile.createNewFile();
			// write file
			out = new FileOutputStream(recFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		audioRec.startRecording();

		// Recoding Thread
		new Thread(new Runnable() {
			public void run() {
				byte buf[] = new byte[bufSize];
				int counter = 0;

				while (bIsRecording) {
					// Read recoding data
					audioRec.read(buf, 0, buf.length);							
					Log.v("AudioRecord", "read " + buf.length + " bytes");

					// change byte order
			        ByteBuffer buffer = ByteBuffer.allocate(buf.length);
			        buffer.put(buf);

			        buffer.order(ByteOrder.BIG_ENDIAN);
			        
			        buf = buffer.array();
			        
					//send sound data 
					SoundManagementNative.sendSoundData(buf, buf.length);

					try {
						out.write(buf);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					//change the file format (raw -> wav)
					addWavHeader();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	@Override
	protected void onDestroy() {
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



	//Save as a WAV header
	public void addWavHeader() throws IOException {
		// The recorded file
		File recFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/rec.raw");
		// WAV file
		File wavFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/rec.wav");

		wavFile.createNewFile();


		// stream
		FileInputStream in = new FileInputStream(recFile);
		FileOutputStream outStream = new FileOutputStream(wavFile);

		// create header
		byte[] header = createHeader(SAMPLING_RATE, (int)recFile.length());
		// write header
		outStream.write(header);

		// read byte data 
		int n = 0,offset = 0;
		byte[] buffer = new byte[(int)recFile.length()];
		while (offset < buffer.length && (n = in.read(buffer, offset, buffer.length - offset)) >= 0) {
			offset += n;
		}
		// write byte data
		outStream.write(buffer);

		// end
		in.close();
		outStream.close();
	}

	//create header of WAV
	public static byte[] createHeader(int sampleRate, int datasize) {
		byte[] byteRIFF = {'R', 'I', 'F', 'F'};
		byte[] byteFilesizeSub8 = intToBytes((datasize + 36));  // file size -8 byte
		byte[] byteWAVE = {'W', 'A', 'V', 'E'};
		byte[] byteFMT_ = {'f', 'm', 't', ' '};
		byte[] byte16bit = intToBytes(16); // fmt chank
		byte[] byteSamplerate = intToBytes(sampleRate);  // sample rate
		byte[] byteBytesPerSec = intToBytes(sampleRate * 2); // byte / seconds = sample rate x 1 channel x 2byte
		byte[] bytePcmMono = {0x01, 0x00, 0x01, 0x00}; // format ID 1 = LPCM , channel 1 = MONORAL
		byte[] byteBlockBit = {0x02, 0x00, 0x10, 0x00}; // Block size 2byte 
		byte[] byteDATA = {'d', 'a', 't', 'a'};
		byte[] byteDatasize = intToBytes(datasize);  // data size

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
	//make change little endian
	public static byte[] intToBytes(int value) {
		byte[] bt = new byte[4];
		bt[0] = (byte)(value & 0x000000ff);
		bt[1] = (byte)((value & 0x0000ff00) >> 8);
		bt[2] = (byte)((value & 0x00ff0000) >> 16);
		bt[3] = (byte)((value & 0xff000000) >> 24);
		return bt;
	}



	}