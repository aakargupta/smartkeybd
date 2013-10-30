
package com.sonyericsson.extras.liveware.extension.keybdsample;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.aef.sensor.Sensor;


import com.sonyericsson.extras.liveware.extension.util.Dbg;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensor;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorEvent;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorEventListener;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorException;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorManager;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorType;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.inputmethodservice.Keyboard.Key;
import android.os.Handler;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The sample control for SmartWatch handles the control on the accessory.
 * This class exists in one instance for every supported host application that
 * we have registered to
 */
class SampleControlSmartWatch extends ControlExtension {

	private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.RGB_565;

	private static final int ANIMATION_X_POS = 46;

	private static final int ANIMATION_Y_POS = 46;

	private static final int ANIMATION_DELTA_MS = 500;

	private Handler mHandler;

	private boolean mIsShowingAnimation = false;

	private boolean mIsVisible = false;

	//private Animation mAnimation = null;

	private final int width;

	private final int height;

	private Bitmap mCurrentImage = null;

	private static final int NUMBER_TILE_TEXT_SIZE = 14;

	private TextPaint mNumberTextPaint;

	private ArrayList<TilePosition> mTilePositions;

	private ArrayList<GameTile> mGameTiles;

	public Process process;
	public DataOutputStream out;

	long pressTime;

	private AccessorySensor mSensor = null;

	private boolean accelOn = false;

	float[] estimates = new float[3];

	float[] cumuEsti = new float[3];

	float[] vestimates = new float[3];

	float[] alpha = new float[3];

	float[] beta = new float[3];

	ArrayList<float[]> estiTrend;

	float[] crossed = new float[3];

	//-1 - Below the cross line. 1 - above
	int[] direction = new int[3];

	long swipeTime;

	long startTime;

	boolean first = false;

	long prevTime;
	
	//for y and z values and the time
	ArrayList<float[]>  prevValues = new ArrayList<float[]>();
	
	ArrayList<Long> prevTimes = new ArrayList<Long>();
	
	//-1 left, 1 - right
	int jerkStart = 0;
	
	//for y and z values and the time
		ArrayList<float[]>  fbprevValues = new ArrayList<float[]>();
		
		ArrayList<Long> fbprevTimes = new ArrayList<Long>();
		
		//-1 left, 1 - right
		int fbjerkStart = 0;
		
		boolean back = false;
	
		String str;
	
	int globalI = 0;
	int globalX;
	int inputLimitCount = 0;
	
	boolean newnavigate = false;
	
	int canstop = 1;
	
	boolean tRunning = false;

	Thread t = new Thread();

	/**
	 * Create sample control.
	 *
	 * @param hostAppPackageName Package name of host application.
	 * @param context The context.
	 * @param handler The handler to use
	 */
	SampleControlSmartWatch(final String hostAppPackageName, final Context context,
			Handler handler) {
		super(context, hostAppPackageName);
		if (handler == null) {
			throw new IllegalArgumentException("handler == null");
		}

		mHandler = handler;
		mNumberTextPaint = new TextPaint();
		mNumberTextPaint.setTextSize(NUMBER_TILE_TEXT_SIZE);
		mNumberTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mNumberTextPaint.setColor(Color.WHITE);
		mNumberTextPaint.setAntiAlias(true);


		width = getSupportedControlWidth(context);
		height = getSupportedControlHeight(context);

		AccessorySensorManager manager = new AccessorySensorManager(context, hostAppPackageName);
		mSensor = manager.getSensor(Sensor.SENSOR_TYPE_ACCELEROMETER);


	}

	/**
	 * Get supported control width.
	 *
	 * @param context The context.
	 * @return the width.
	 */
	public static int getSupportedControlWidth(Context context) {
		return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_control_width);
	}

	/**
	 * Get supported control height.
	 *
	 * @param context The context.
	 * @return the height.
	 */
	public static int getSupportedControlHeight(Context context) {
		return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_control_height);
	}

	@Override
	public void onDestroy() {

		Log.d(SampleExtensionService.LOG_TAG, "SampleControlSmartWatch onDestroy");
		//stopAnimation();
		mHandler = null;

		// Stop sensor
		if (mSensor != null) {
			mSensor.unregisterListener();
			mSensor = null;
		}

	};

	@Override
	public void onStart() {
		// Nothing to do. Animation is handled in onResume.
		try
		{
			process = Runtime.getRuntime().exec("su");
			out = new DataOutputStream(process.getOutputStream());
		}
		catch(Exception e)
		{
			Dbg.d("XYZY: process ");
			Log.d("Singleton", "XYZY: gfdhf");
		}
	}

	@Override
	public void onStop() {
		// Nothing to do. Animation is handled in onPause.

	}

	@Override
	public void onResume() {
		mIsVisible = true;

		Log.d(SampleExtensionService.LOG_TAG, "Starting animation");

		startNewGame();

		//0.15322891 is the smallest unit of measurement
		estimates[0] = (float)0.15322891;
		estimates[1] = (float)0.15322891;
		estimates[2] = (float)9.65322891;

		vestimates[0] = 0;
		vestimates[1] = 0;
		vestimates[2] = 0;        
		
		newnavigate = false;
		canstop = 1;
		tRunning=false;
		str = "";
		globalI = 0;

		alpha[0] = (float)0.05;
		alpha[1] = (float)0.05;
		alpha[2] = (float)0.05;

		beta[0] = (float)0.001;
		beta[1] = (float)0.001;
		beta[2] = (float)0.001;
		
		//direction[0] = 0;
		//direction[1] = 0;
		//direction[2] = 0;
		globalX = 750;
		inputLimitCount=0;

		first = false;

		estiTrend = new ArrayList<float[]>();
		
		
		prevValues = new ArrayList<float[]>();		
		jerkStart = 0;
		prevTimes = new ArrayList<Long>();
		
		
		fbprevValues = new ArrayList<float[]>();		
		fbjerkStart = 0;
		fbprevTimes = new ArrayList<Long>();
		
		
		prevTime = -1;
		setScreenState(Control.Intents.SCREEN_STATE_ON);
		// Animation not showing. Show animation.
		mIsShowingAnimation = true;
		// mAnimation = new Animation();
		//  mAnimation.run();

		// Start listening for sensor updates.
		if (mSensor != null) {
			try {

				mSensor.registerFixedRateListener(mListener, Sensor.SensorRates.SENSOR_DELAY_FASTEST);
			} catch (AccessorySensorException e) {
				Log.d(SampleExtensionService.LOG_TAG, "Failed to register listener");
			}
		}
	}

	
	@Override
	public void onPause() {
		Log.d(SampleExtensionService.LOG_TAG, "Stopping animation");
		mIsVisible = false;

		if (mIsShowingAnimation) {
			stopAnimation();

		}
	//	String s="sendevent /dev/input/event2 3 57 4294967295\n"+"sendevent /dev/input/event2 0 0 0\n";
		
		/*try{
			out.writeBytes(s);
			out.flush();
			
		}
		catch(IOException e){
			Log.d("Accel", "EXC: Pau");
		}*/
		setScreenState(Control.Intents.SCREEN_STATE_AUTO);
		// Stop sensor
		if (mSensor != null) {
			mSensor.unregisterListener();
		}
	}
	
	private final AccessorySensorEventListener mListener = new AccessorySensorEventListener() {

		public void onSensorEvent(AccessorySensorEvent sensorEvent) {
			processSensor(sensorEvent);
		}		
	};

	private void processSensor(AccessorySensorEvent sensorEvent) {


		// Process the values.
		if (sensorEvent != null && accelOn==true) {

			float[] values = sensorEvent.getSensorValues();

			if (values != null && values.length == 3) {

				setScreenState(Control.Intents.SCREEN_STATE_ON);

				long currTime = sensorEvent.getTimestamp();
				if (prevTime == -1)
				{
					prevTime = currTime;
					estimates[0] = values[0];
					estimates[1] = values[1];
					estimates[2] = values[2];

					cumuEsti[0] = 0;
					cumuEsti[1] = 0;
					cumuEsti[2] = 0;

					swipeTime = currTime;
					startTime = currTime;

					crossed[0] = estimates[0];
					crossed[1] = estimates[1];
					crossed[2] = estimates[2];

					direction[0] = 0;
					direction[1] = 0;
					direction[2] = 0;       	        

				}
				else
				{
					// TextView xView = (TextView)sampleLayout.findViewById(R.id.accelerometer_value_x);
					// TextView yView = (TextView)sampleLayout.findViewById(R.id.accelerometer_value_y);
					// TextView zView = (TextView)sampleLayout.findViewById(R.id.accelerometer_value_z);

					// Show values with one decimal.
					//xView.setText(String.format("%.1f", values[0]));
					//yView.setText(String.format("%.1f", values[1]));
					//zView.setText(String.format("%.1f", values[2]));

					//Log.d("Accel", "ACEL: "+values[0]+ " "+values[1]+" "+values[2]+" "+sensorEvent.getAccuracy());

					if(first==false)
					{
						estimates[0] = values[0];
						estimates[1] = values[1];
						estimates[2] = values[2];

						crossed[0] = estimates[0];
						crossed[1] = estimates[1];
						crossed[2] = estimates[2];
						first = true;
					}


					float[] x = new float[3];
					float[] v = new float[3];
					float[] r = new float[3];

					long dt = currTime-prevTime;
					dt/=1000;
					float[] temp = new float[3];
					for(int i=0;i<3;i++)
					{   temp[i] = estimates[i];     				        				
					x[i] = estimates[i]+dt*vestimates[i];
					v[i] = vestimates[i];
					r[i] = values[i]-x[i];

					x[i] = x[i]+alpha[i]*r[i];
					v[i] = v[i]+beta[i]/dt*r[i];

					estimates[i] = x[i];
					vestimates[i] = v[i];

					Log.d("Jerk", "JERK: "+values[0]+ " "+values[1]+" "+values[2]+" "+estimates[0]+ " "+estimates[1]+" "+estimates[2]+" "+vestimates[0]+ " "+vestimates[1]+" "+vestimates[2]);

					cumuEsti[i] += estimates[i]-temp[i];

					//navigate(currTime);
					//jerk(values[1], values[2], currTime);
					fwdbwd(values[0], values[1], values[2], currTime);
					}

					//estiTrend.add(estimates);        			

				}
			}      
		}		
	}	

	private void fwdbwd(float x, float y, float z, long currTime) {
		int i = fbprevValues.size();
		boolean jerkFinish = false; 
		while(i>0)
		{
			float[] temp = fbprevValues.get(i-1);
			long tempTime = fbprevTimes.get(i-1);
			
			//Log.d("Jerk", "AA: "+i+" "+currTime+" "+tempTime);
			//Log.d("Jerk", "AA: "+i+" "+y+" "+temp[0]+" "+z+" "+temp[1]);
			if(currTime<=tempTime+200000000)
			{
				//Log.d("Jerk", "XAX: "+y+" "+temp[0]+" "+z+" "+temp[1]);
				if (x>(temp[0]+10) && Math.abs(y-temp[1])<Math.abs(x-temp[0])/1.5)
				{
					//Log.d("Jerk", "AA: 5");
					if (fbjerkStart==0)
					{
						fbjerkStart = 1;
						
						Log.d("Jerk", "AA: 1");

					}
					else
					{
						Log.d("Jerk", "AB: 11");
						executeFB(1); //right
						fbprevValues = new ArrayList<float[]>();
						fbprevTimes = new ArrayList<Long>();
						fbjerkStart = 0;
						jerkFinish = true;
						Log.d("Jerk", "AA: 2");
						
					}
					break;
				}
				else if (x<(temp[0]-10) && Math.abs(y-temp[1])<Math.abs(x-temp[0])/1.5)
				{
					Log.d("Jerk", "AA: 6");
					if (fbjerkStart==0)
					{
						fbjerkStart = -1;
						
						Log.d("Jerk", "AA: 3");
					}
					else
					{
						Log.d("Jerk", "AB: 12");
						executeFB(-1); //left
						fbprevValues = new ArrayList<float[]>();
						fbprevTimes = new ArrayList<Long>();
						fbjerkStart = 0;
						jerkFinish = true;
						Log.d("Jerk", "AA: 4");
					}
					break;
				}
			}
			else
			{
				jerkStart = 0;
				for(int j=0; j<i;j++)
				{
					//since list keeps getting updated
					fbprevValues.remove(0);
					fbprevTimes.remove(0);
				}
				break;
			}
					
			i--;
		}
		if(!jerkFinish)
		{
			float[] t = new float[3];
			t[0] = x;
			t[1] = y;
			t[2] = z;
			fbprevTimes.add(currTime);
			fbprevValues.add(t);
		}		
		Log.d("Jerk", "AXA: ");
		
	}

	private void executeFB(int i) {
		try
		{
			int key = KeyEvent.KEYCODE_CONTACTS;
			if(i>0)
			{
				key = KeyEvent.KEYCODE_BACK;
			}
			out.writeBytes("input keyevent "+key+"\n");					 
			out.flush();
			//process.waitFor();
		}
		catch (Exception e)
		{
			Dbg.d("KKFFF"+e.getMessage());
		}   
		
	}

	private void jerk(float y, float z, long currTime) {
		
		int i = prevValues.size();
		boolean jerkFinish = false; 
		while(i>0)
		{
			float[] temp = prevValues.get(i-1);
			long tempTime = prevTimes.get(i-1);
			
			Log.d("Jerk", "AA: "+i+" "+currTime+" "+tempTime);
			Log.d("Jerk", "AA: "+i+" "+y+" "+temp[0]+" "+z+" "+temp[1]);
			if(currTime<=tempTime+200000000)
			{
				Log.d("Jerk", "AAY: "+y+" "+temp[0]+" "+z+" "+temp[1]);
				if (y>(temp[0]+10) && z<(temp[1]-1))
				{
					Log.d("Jerk", "AA: 5");
					if (jerkStart==0)
					{
						jerkStart = 1;
						
						Log.d("Jerk", "AA: 1");

					}
					else
					{
						executeJerk(1); //right
						prevValues = new ArrayList<float[]>();
						prevTimes = new ArrayList<Long>();
						jerkStart = 0;
						jerkFinish = true;
						Log.d("Jerk", "AA: 2");
					}
					break;
				}
				else if (y<(temp[0]-9) && z<(temp[1]-4))
				{
					Log.d("Jerk", "AA: 6");
					if (jerkStart==0)
					{
						jerkStart = -1;
						
						Log.d("Jerk", "AA: 3");
					}
					else
					{
						executeJerk(-1); //left
						prevValues = new ArrayList<float[]>();
						prevTimes = new ArrayList<Long>();
						jerkStart = 0;
						jerkFinish = true;
						Log.d("Jerk", "AA: 4");
					}
					break;
				}
			}
			else
			{
				jerkStart = 0;
				for(int j=0; j<i;j++)
				{
					//since list keeps getting updated
					prevValues.remove(0);
					prevTimes.remove(0);
				}
				break;
			}
					
			i--;
		}
		if(!jerkFinish)
		{
			float[] t = new float[2];
			t[0] = y;
			t[1] = z;
			prevTimes.add(currTime);
			prevValues.add(t);
		}		
		Log.d("Jerk", "AXA: ");
	}

	private void executeJerk(int i) {
		try
		{
			int start=10, finish=500;
			if(i>0)
			{
				start = 500;
				finish = 10;
			}
			out.writeBytes("input swipe "+start+" 200 "+finish+" 200\n");					 
			out.flush();
			//process.waitFor();
		}
		catch (Exception e)
		{
			Dbg.d("KKFFF"+e.getMessage());
		}   
		
	}

	private void startNewGame() {
		//drawLoadingScreen();

		// Create game positions
		initTilePositions(new TilePosition(1, new Rect(0, 32, 31, 63)), new TilePosition(2,
				new Rect(32, 32, 63, 63)), new TilePosition(3, new Rect(64, 32, 95, 63)),  new TilePosition(4, new Rect(96, 32, 127, 63)),				
						new TilePosition(5, new Rect(0, 64, 31, 95)), new TilePosition(6,
				new Rect(32, 64, 63, 95)), new TilePosition(7, new Rect(64, 64, 95, 95)),  new TilePosition(8, new Rect(96, 64, 127, 95)),
						new TilePosition(9, new Rect(0, 96, 31, 127)), new TilePosition(10,
				new Rect(32, 96, 63, 127)), new TilePosition(11, new Rect(64, 96, 95, 127)),  new TilePosition(12, new Rect(96, 96, 127, 127)),
						new TilePosition(13, new Rect(96, 0, 127, 31)));
				
				
				mCurrentImage = getNumberImage();


		// Create game tiles
		initTiles();



		// Draw initial game Bitmap
		getCurrentImage(true);

		// Init game state
		// mNumberOfMoves = 0;
		// mGameState = GameState.PLAYING;
		// Dbg.d("game started with empty tile index " + mEmptyTileIndex);
	}


	/**
	 * Init the 9 tile position objects.
	 *
	 * @param tilePositions The tile positions
	 */
	private void initTilePositions(TilePosition... tilePositions) {
		mTilePositions = new ArrayList<TilePosition>(13);
		for (TilePosition tilePosition : tilePositions) {
			mTilePositions.add(tilePosition);
		}
	}

	/**
	 * Get bitmap with number tiles drawn.
	 *
	 * @return The bitmap
	 */
	private Bitmap getNumberImage() {
		Bitmap bitmap = Bitmap.createBitmap(width, height, BITMAP_CONFIG);
		// Set the density to default to avoid scaling.
		bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);

		Paint tilePaint = new Paint();
		tilePaint.setColor(Color.GRAY);
		for (TilePosition tilePosition : mTilePositions) {
			//if (tilePosition.position != 25) {
			canvas.drawRect(tilePosition.frame, tilePaint);
			canvas.drawText(tilePosition.position,
					tilePosition.frame.left + 5, tilePosition.frame.top + 18, mNumberTextPaint);
			//}
		}

		return bitmap;
	}

	/**
	 * Init the 9 tiles with index and bitmap, based on game type.
	 */
	private void initTiles() {
		mGameTiles = new ArrayList<GameTile>(13);
		// Force size to 9
		for (int i = 0; i < 13; i++) {
			mGameTiles.add(new GameTile());
		}

		int i = 1;
		for (TilePosition tp : mTilePositions) {
			GameTile gt = new GameTile();
			if (i != 14) {
				gt.correctPosition = i;

				char[] ls = "0ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
				String r = "";
				while(true) {
					r =String.valueOf(ls[(i * 2)-1])+String.valueOf(ls[i * 2]) + r;
					if(i < 26) {
						break;
					}
					i /= 26;
				}

				gt.text = r;
				Log.d("Keybd", "Keybd: "+r);
			}

			gt.tilePosition = tp;
			//  if (mGameType == GameType.NUMBERS) {
			setNumberTile(gt);
			//  } else {
			//      setImageTile(mCurrentImage, gt);
			//  }
			mGameTiles.set(i-1, gt);
			i++;
		}
	}

	/**
	 * Create number based bitmap for tile
	 *
	 * @param gt The tile
	 */
	private void setNumberTile(GameTile gt) {
		gt.bitmap = Bitmap.createBitmap(32, 32, BITMAP_CONFIG);
		// Set the density to default to avoid scaling.
		gt.bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

		Canvas canvas = new Canvas(gt.bitmap);
		//if (gt.text != null) {
		canvas.drawColor(Color.GRAY);
		canvas.drawText(gt.text, 5, 18, mNumberTextPaint);
		// } else {
		// Empty tile
		//    canvas.drawColor(Color.WHITE);
		//    mEmptyTileIndex = gt.tilePosition.position;
		//  }
	}

	/**
	 * Draw all tiles into bitmap and show it.
	 *
	 * @param show True if bitmap shown be shown, false otherwise
	 * @return The complete bitmap of the current game
	 */
	private Bitmap getCurrentImage(boolean show) {
		Bitmap bitmap = Bitmap.createBitmap(width, height, BITMAP_CONFIG);
		bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
		Canvas canvas = new Canvas(bitmap);
		// Set background
		canvas.drawColor(Color.BLACK);
		// Draw tiles
		for (GameTile gt : mGameTiles) {
			canvas.drawBitmap(gt.bitmap, gt.tilePosition.frame.left, gt.tilePosition.frame.top,
					null);
		}
		if (show) {
			showBitmap(bitmap);
		}

		return bitmap;
	}

	/**
	 * Stop showing animation on control.
	 */
	public void stopAnimation() {
		// Stop animation on accessory
		// if (mAnimation != null) {
		//  mAnimation.stop();
		//  mHandler.removeCallbacks(mAnimation);
		//   mAnimation = null;
		//  }
		mIsShowingAnimation = false;

		// If the control is visible then stop it
		if (mIsVisible) {
			stopRequest();
		}
	}

	@Override
	public void onTouch(final ControlTouchEvent event) {
		//Log.d(SampleExtensionService.LOG_TAG, "onTouch() " + event.getAction());
		
		if (event.getAction() == Control.Intents.KEY_ACTION_PRESS) {
			pressTime = event.getTimeStamp();
			Log.d("Keybd", "Keybd: "+event.getX()+" ,"+event.getY());
		}
		else if (event.getAction() == Control.Intents.TOUCH_ACTION_RELEASE) {
			int tileReleaseIndex = getTileIndex(event);

			if(tileReleaseIndex == -1)
				return;
			char[] ls = "0ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
			String r = "";
			while(true) {
				r = ls[(tileReleaseIndex*2)-1] + r;
				if(tileReleaseIndex < 14) {
					break;
				}
				tileReleaseIndex /= 26;
			} 
			Log.d("Keybd", "Keybd: "+event.getX()+" ,"+event.getY()+"\n");

			int val=1;
			if ((event.getTimeStamp()-pressTime)>200)
				val = 0;
			
			int keyindex = ((tileReleaseIndex*2)-val)+28; 
			try
			{
				out.writeBytes("input keyevent "+ keyindex+"\n");
				//out.writeBytes("input tap 500 600\n");


				//out.writeBytes("mv /system/file.old system/file.new\n");
				//out.writeBytes("exit\n");  
				out.flush();
				//process.waitFor();
			}
			catch (Exception e)
			{
				Dbg.d("KKFFF"+e.getMessage());
			}   
		}
	}

	@Override
	public void onSwipe(int direction) {

		switch (direction) {
		case Control.Intents.SWIPE_DIRECTION_LEFT:
			try
			{
				out.writeBytes("input keyevent 67\n");
				//out.writeBytes("input tap 500 600\n");


				//out.writeBytes("mv /system/file.old system/file.new\n");
				//out.writeBytes("exit\n");  
				out.flush();
				//process.waitFor();
			}
			catch (Exception e)
			{
				Dbg.d("KKFFF"+e.getMessage());
			}
		case Control.Intents.SWIPE_DIRECTION_RIGHT:

			try
			{
				out.writeBytes("input keyevent 62\n");
				//out.writeBytes("input tap 500 600\n");


				//out.writeBytes("mv /system/file.old system/file.new\n");
				//out.writeBytes("exit\n");  
				out.flush();
				//process.waitFor();
			}
			catch (Exception e)
			{
				Dbg.d("KKFFF"+e.getMessage());
			}
			break;
		case Control.Intents.SWIPE_DIRECTION_UP:
			if (accelOn == false)
			{
				accelOn = true;
				Log.d("Accel", "ACEL: on");
				//startVibrator(40, 0, 1);

			}

			else
			{
				accelOn = false;
				Log.d("Accel", "ACEL: off");
				//startVibrator(30, 30, 2);
			}
			/*try
			{
				out.writeBytes("input keyevent 62\n");
				//out.writeBytes("input tap 500 600\n");


				//out.writeBytes("mv /system/file.old system/file.new\n");
				//out.writeBytes("exit\n");  
				out.flush();
				//process.waitFor();
			}
			catch (Exception e)
			{
				Dbg.d("KKFFF"+e.getMessage());
			}*/
			break;
		default:
			break;
		}

	}

	/**
	 * Start repeating vibrator
	 *
	 * @param onDuration On duration in milliseconds.
	 * @param offDuration Off duration in milliseconds.
	 * @param repeats The number of repeats of the on/off pattern. Use
	 *            {@link Control.Intents#REPEAT_UNTIL_STOP_INTENT} to repeat
	 *            until explicitly stopped.
	 */
	public void startVibrator(int onDuration, int offDuration, int repeats) {
		if (Dbg.DEBUG) {
			Dbg.v("startVibrator: onDuration: " + onDuration + ", offDuration: " + offDuration
					+ ", repeats: " + repeats);
		}
		Intent intent = new Intent(Control.Intents.CONTROL_VIBRATE_INTENT);
		intent.putExtra(Control.Intents.EXTRA_ON_DURATION, onDuration);
		intent.putExtra(Control.Intents.EXTRA_OFF_DURATION, offDuration);
		intent.putExtra(Control.Intents.EXTRA_REPEATS, repeats);
		sendToHostApp(intent);
	}

	/**
	 * Get tile index for the coordinates in the event.
	 *
	 * @param event The touch event
	 * @return The tile index
	 */
	private int getTileIndex(ControlTouchEvent event) {
		int x = event.getX();
		int y = event.getY();

		//Finger correction
		//if (x > 5)
			//x = x-5;
		//if (y>2)
			//y = y-2;
		int rowIndex = x / 32;
		int columnIndex = y / 32;
		if (columnIndex==0)
		{
			if (rowIndex==3)
				return 13;
			else
				return -1;
		}
		else
			return 1+rowIndex + (columnIndex-1) * 4;
	}
	/* *//**
	 * The animation class shows an animation on the accessory. The animation
	 * runs until mHandler.removeCallbacks has been called.
	 *//*
    private class Animation implements Runnable {
        private int mIndex = 1;

        private final Bitmap mBackground;

        private boolean mIsStopped = false;

	  *//**
	  * Create animation.
	  *//*
        Animation() {
            mIndex = 1;

            // Extract the last part of the host application package name.
            String packageName = mHostAppPackageName
                    .substring(mHostAppPackageName.lastIndexOf(".") + 1);

            // Create background bitmap for animation.
            mBackground = Bitmap.createBitmap(width, height, BITMAP_CONFIG);
            // Set default density to avoid scaling.
            mBackground.setDensity(DisplayMetrics.DENSITY_DEFAULT);

            LinearLayout root = new LinearLayout(mContext);
            root.setLayoutParams(new LayoutParams(width, height));

            LinearLayout sampleLayout = (LinearLayout)LinearLayout.inflate(mContext,
                    R.layout.sample_control, root);
            ((TextView)sampleLayout.findViewById(R.id.sample_control_text)).setText(packageName);
            sampleLayout.measure(width, height);
            sampleLayout.layout(0, 0, sampleLayout.getMeasuredWidth(),
                    sampleLayout.getMeasuredHeight());

            Canvas canvas = new Canvas(mBackground);
            sampleLayout.draw(canvas);

            showBitmap(mBackground);
        }

	   *//**
	   * Stop the animation.
	   *//*
        public void stop() {
            mIsStopped = true;
        }

        public void run() {
            int resourceId;
            switch (mIndex) {
                case 1:
                    resourceId = R.drawable.generic_anim_1_icn;
                    break;
                case 2:
                    resourceId = R.drawable.generic_anim_2_icn;
                    break;
                case 3:
                    resourceId = R.drawable.generic_anim_3_icn;
                    break;
                case 4:
                    resourceId = R.drawable.generic_anim_2_icn;
                    break;
                default:
                    Log.e(SampleExtensionService.LOG_TAG, "mIndex out of bounds: " + mIndex);
                    resourceId = R.drawable.generic_anim_1_icn;
                    break;
            }
            mIndex++;
            if (mIndex > 4) {
                mIndex = 1;
            }

            if (!mIsStopped) {
                updateAnimation(resourceId);
            }
            if (mHandler != null && !mIsStopped) {
                mHandler.postDelayed(this, ANIMATION_DELTA_MS);
            }
        }

	    *//**
	    * Update the animation on the accessory. Only updates the part of the
	    * screen which contains the animation.
	    *
	    * @param resourceId The new resource to show.
	    *//*
        private void updateAnimation(int resourceId) {
            Bitmap animation = BitmapFactory.decodeResource(mContext.getResources(), resourceId,
                    mBitmapOptions);

            // Create a bitmap for the part of the screen that needs updating.
            Bitmap bitmap = Bitmap.createBitmap(animation.getWidth(), animation.getHeight(),
                    BITMAP_CONFIG);
            bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            Rect src = new Rect(ANIMATION_X_POS, ANIMATION_Y_POS, ANIMATION_X_POS
                    + animation.getWidth(), ANIMATION_Y_POS + animation.getHeight());
            Rect dst = new Rect(0, 0, animation.getWidth(), animation.getHeight());

            // Add first the background and then the animation.
            canvas.drawBitmap(mBackground, src, dst, paint);
            canvas.drawBitmap(animation, 0, 0, paint);

            showBitmap(bitmap, ANIMATION_X_POS, ANIMATION_Y_POS);
        }
    };*/

}
