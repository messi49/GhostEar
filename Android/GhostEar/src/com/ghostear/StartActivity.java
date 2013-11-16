package com.ghostear;

import com.ghostear.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class StartActivity extends Activity implements OnClickListener {
	//Start Button
	Button startButton;
	// address box and port number box
	EditText addressBox, portNumberBox;
	// address(string) and port(string)
	String address, port;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);

		// start button
		startButton = (Button)findViewById(R.id.startButton);
		startButton.setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.start, menu);
		return true;
	}

	public void onClick(View v) {
		// push button
		if(v == startButton){
			// show the message
			Toast.makeText(this, "Start Push!", Toast.LENGTH_LONG).show();

			// pass intent to GhostEyeActivity
			Intent intent = new Intent(getApplication(), SoundManagementActivity.class);

			// address box
			addressBox = (EditText) findViewById(R.id.addressBox);
			address = addressBox.getText().toString();

			// port number box
			portNumberBox = (EditText) findViewById(R.id.portNumberBox);
			port = portNumberBox.getText().toString();

			// address and port
			intent.putExtra("address", address);
			intent.putExtra("port", 0);

			// start
			startActivity(intent);
		}
	}
}