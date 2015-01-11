package me.studnicka.glassmovieplayer;

import android.content.Context;
import android.media.AudioManager;
import com.google.android.glass.media.Sounds;

public class SoundManager {
    private AudioManager mAudioManager;
	
	public enum SoundId { TAP, DISMISS, VIDEO_START, VIDEO_STOP, VOLUME_CHANGE }
	
	public SoundManager(Context context) {
        mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
	}
	
	public void playSound(SoundId soundId) {
		switch(soundId) {
		case TAP:
            mAudioManager.playSoundEffect(Sounds.TAP);
			break;
		case DISMISS:
            mAudioManager.playSoundEffect(Sounds.DISMISSED);
			break;
		case VIDEO_START:
            mAudioManager.playSoundEffect(Sounds.SELECTED);
			break;
		case VIDEO_STOP:
            mAudioManager.playSoundEffect(Sounds.SUCCESS);
			break;
		case VOLUME_CHANGE:
            mAudioManager.playSoundEffect(Sounds.SELECTED);
		}
	}

    // --- Volume

    public int getMaxVolume() {
        return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public int readAudioVolume() {
        return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public void writeAudioVolume(int volume) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

}
