package com.couchbase.grocerysync;

import android.app.Dialog;
import android.content.Context;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SplashScreenDialog extends Dialog {
	
	protected ProgressBar splashProgressBar;
	protected TextView splashProgressMessage;

	public SplashScreenDialog(Context context) {
		super(context, R.style.SplashScreenStyle);
		
		setContentView(R.layout.splashscreen);
		setCancelable(false);
		
	    splashProgressBar = (ProgressBar)findViewById(R.id.splashProgressBar);
	    splashProgressBar.setProgress(0);
	    splashProgressBar.setMax(100);
	    
	    splashProgressMessage = (TextView)findViewById(R.id.splashProgressMessage);
	    splashProgressMessage.setText(context.getString(R.string.startup_message));
	}
	
	/**
	 * Logic to control progress and message together
	 */
	public void updateSplashScreenProgress(int completed, int total) {
		if(completed < (total - 1)) {
			updateSplashScreenProgressBar(completed, total);
			updateSplashScreenProgressMessage(getContext().getString(R.string.installing_message));
		}
		else {
			updateSplashScreenProgressBar(completed, total);
			updateSplashScreenProgressMessage(getContext().getString(R.string.startup_message));
		}
	}
	
	/**
	 * Update the Splash Screen Progress Bar
	 */	
	private void updateSplashScreenProgressBar(int progress, int max) {
		if(splashProgressBar != null) {
			splashProgressBar.setProgress(progress);
			splashProgressBar.setMax(max);
		}
	}
	
	/**
	 * Update the Splash Screen Progress Message
	 */
	private void updateSplashScreenProgressMessage(String message) {
		if(splashProgressMessage != null) {
			splashProgressMessage.setText(message);
		}
	}

}
