package com.ghostear;

public class SoundManagementNative {
	static{
		System.loadLibrary("soundsender");
	}
	public static native void connectServer(String address, int port_number);
	public static native void closeConnect();
	public static native void sendSoundData(byte buf[], int size);
}
