package me.studnicka.glassmovieplayer;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.touchpad.GestureDetector.BaseListener;
import com.google.android.glass.touchpad.GestureDetector.FingerListener;
import com.google.android.glass.touchpad.GestureDetector.ScrollListener;
import me.studnicka.glassmovieplayer.SoundManager.SoundId;

public class VolumeDialog extends Dialog {
	private Context mContext;
	private ImageView mVolumeIcon;
	private SeekBar mVolumeSeek;
	private GestureDetector mGestureDetector;
	private int mVolume;
	private SoundManager mSoundManager;
	private Handler mHandler;
	private int mNumVolumeValues;

	public VolumeDialog(Context context) {
		super(context);
		mContext = context;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_volume);

		mVolumeIcon = (ImageView)findViewById(R.id.volume_icon);
		mVolumeSeek = (SeekBar)findViewById(R.id.volume_seek);

        mSoundManager = new SoundManager(mContext);

		mNumVolumeValues = mSoundManager.getMaxVolume();
		mVolumeSeek.setMax(mNumVolumeValues - 1);
		mVolumeSeek.setOnSeekBarChangeListener(mSeekBarChangeListener);

		mVolume = mSoundManager.readAudioVolume();
		updateVolumeDrawable(mVolume);
		
		mVolumeSeek.setProgress(mVolume);


		// used to move the seekbar. temporary solution until
		// the GDK improves
		mGestureDetector = new GestureDetector(mContext);
		mGestureDetector.setBaseListener(mBaseListener);
		mGestureDetector.setFingerListener(mFingerListener);
		mGestureDetector.setScrollListener(mScrollListener);
		mHandler = new Handler();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		mContext.registerReceiver(mHeadsetReceiver, new IntentFilter("android.intent.action.HEADSET_PLUG"));
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		mContext.unregisterReceiver(mHeadsetReceiver);
	}
	
	private OnSeekBarChangeListener mSeekBarChangeListener = new OnSeekBarChangeListener() {
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			updateVolumeDrawable(progress);
            mSoundManager.writeAudioVolume(progress);
		}
	};
	
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		super.onGenericMotionEvent(event);
		return mGestureDetector.onMotionEvent(event);
	}
	
	boolean mIsDismissing;
	
	private BaseListener mBaseListener = new BaseListener() {
		
		@Override
		public boolean onGesture(Gesture gesture) {
			if(gesture == Gesture.TAP) {
				dismiss();
				return true;
			} else {
				return false;
			}
		}
	};
	
	private FingerListener mFingerListener = new FingerListener() {
		
		@Override
		public void onFingerCountChanged(int previousCount, int currentCount) {
			if(previousCount > 0 && currentCount == 0) {
				mVolume = mVolumeSeek.getProgress();
				
				mHandler.removeCallbacks(mDingRunnable);
				mHandler.postDelayed(mDingRunnable, 100);
			}
		}
	};
	
	private Runnable mDingRunnable = new Runnable() {
		
		@Override
		public void run() {
			if(isShowing()) {
			    mSoundManager.playSound(SoundId.VOLUME_CHANGE);
			}
		}
	};
	
	private ScrollListener mScrollListener = new ScrollListener() {
		
		@Override
		public boolean onScroll(float displacement, float delta, float velocity) {
			int value = (int)(mVolume + displacement / 100);
			if(value >= mNumVolumeValues) {
				value = mNumVolumeValues - 1;
			}
	
			if(value < 0) {
				value = 0;
			}

			mVolumeSeek.setProgress(value);
			
		    return true;
		}
	}; 
	
	private void updateVolumeDrawable(int volume) {
		int icon;
	    if(volume == 0) {
	    	icon = R.drawable.ic_volume_0_large;
	    } else if(volume < mNumVolumeValues/2) {
	    	icon = R.drawable.ic_volume_1_large;
	    } else {
	    	icon = R.drawable.ic_volume_2_large;
	    }
	    mVolumeIcon.setImageResource(icon);
	}
	
	private BroadcastReceiver mHeadsetReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			mNumVolumeValues = mSoundManager.getMaxVolume();
			mVolumeSeek.setMax(mNumVolumeValues - 1);
		}
	};

}
