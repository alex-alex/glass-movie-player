package me.studnicka.glassmovieplayer;

import java.util.ArrayList;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.glass.widget.CardScrollView;
import me.studnicka.glassmovieplayer.SoundManager.SoundId;

public class MoviePickerActivity extends Activity implements LoaderCallbacks<Cursor> {
	public static final String EXTRA_MOVIE_BUCKET = "movie bucket";
	private static final int URL_LOADER = 0;
	private Cursor mMovieCursor;
	private CardScrollView mList;
	private MovieAdapter mAdapter;
	private View mEmptyMessage;
	private int mLength;
	private ProgressBar mDeleteProgress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_movie_picker);
		mLength = -1;
		mList = (CardScrollView)findViewById(R.id.list);
		mEmptyMessage = findViewById(R.id.empty);
		mDeleteProgress = (ProgressBar)findViewById(R.id.progress);
		
		mList.setOnItemClickListener(mItemClickListener);
		
        getLoaderManager().initLoader(URL_LOADER, null, this);
        
        mList.activate();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.video_picker, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(mLength > 0) {
			menu.findItem(R.id.play).setVisible(true).setEnabled(true);
			menu.findItem(R.id.delete).setVisible(true).setEnabled(true);
		} else {
			menu.findItem(R.id.play).setVisible(false).setEnabled(false);
			menu.findItem(R.id.delete).setVisible(false).setEnabled(false);
		}
		
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.play:
			play();
			return true;
		case R.id.auto_play:
			autoPlay();
			return true;
		case R.id.delete:
			delete();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private OnItemClickListener mItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			getSoundManager().playSound(SoundId.TAP);
			openOptionsMenu();
		}
	};

	private void play() {
		int position = mList.getSelectedItemPosition();
		
		if(mLength > 0 && position != -1) {
			int index = mMovieCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
			mMovieCursor.moveToPosition(position);
			String videoLocationPath = mMovieCursor.getString(index);
			Uri videoLocation = Uri.parse(videoLocationPath);
			
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(videoLocation, "video/*");
			getSoundManager().playSound(SoundId.VIDEO_START);
			startActivity(intent);
		} 
	}
	
	private void autoPlay() {
		int position = mList.getSelectedItemPosition();
		
		if(mLength > 0 && position != -1) {
			int index = mMovieCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
			
			ArrayList<CharSequence> videoList = new ArrayList<CharSequence>();
			
			mMovieCursor.moveToPosition(position);
			do {
				String videoLocationPath = mMovieCursor.getString(index);
				videoList.add(videoLocationPath);
			} while(mMovieCursor.moveToNext());
			
			Intent intent = new Intent(this, MoviePlayerActivity.class);
			intent.putCharSequenceArrayListExtra(MoviePlayerActivity.EXTRA_PLAYLIST, videoList);
			startActivity(intent);
		}
		
	}
	
	private void delete() {
		int position = mList.getSelectedItemPosition();
		
		if(mLength > 0 && position != -1) {
			mMovieCursor.moveToPosition(position);
			
			int idIndex = mMovieCursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
			long videoId = mMovieCursor.getLong(idIndex);
			Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
			Uri itemUri = ContentUris.withAppendedId(contentUri, videoId);
			
			DeleteHandler deleteHandler = new DeleteHandler(getContentResolver(), mDeleteProgress);
			deleteHandler.startDelete(0, this, itemUri, null, null);
		}
	}
	
	private static class DeleteHandler extends AsyncQueryHandler {
		private ProgressBar mProgress;

		public DeleteHandler(ContentResolver cr, ProgressBar progress) {
			super(cr);
			mProgress = progress;
			progress.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected void onDeleteComplete(int token, Object cookie, int result) {
			super.onDeleteComplete(token, cookie, result);
			
			mProgress.setVisibility(View.INVISIBLE);
			
			Context context = (Context)cookie;
			
			if(result == 0) {
				Toast.makeText(context, "Error. Could not delete video.", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(context, "Video deleted", Toast.LENGTH_SHORT).show();
			}
		}
		
	}
	
    private SoundManager getSoundManager() {
      return ((GlassApplication)getApplication()).getSoundManager();
    }

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
		switch(loaderId) {
		case URL_LOADER:
			String[] proj = { MediaStore.Video.Media._ID,
					MediaStore.Video.Media.ALBUM,
					MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
					MediaStore.Video.Media.DATA,
					MediaStore.Video.Media.DISPLAY_NAME,
					MediaStore.Video.Media.SIZE };
			
			long bucketId = getIntent().getLongExtra(EXTRA_MOVIE_BUCKET, 0L);
			String selection = null;
			String[] selectionArgs = null;
			
			if(bucketId != 0) {
				selection = MediaStore.Video.Media.DATA + " not like ? and " + MediaStore.Video.Media.BUCKET_ID + " =? " ;
			    selectionArgs = new String[] {"%sdcard/glass_cached_files%", Long.toString(bucketId) };
			} else {
				selection = MediaStore.Video.Media.DATA + " not like ? ";
			    selectionArgs = new String[] { "%sdcard/glass_cached_files%" };
			}

			return new CursorLoader(this, MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
					proj, selection, selectionArgs, MediaStore.Video.Media.DISPLAY_NAME);

		default:
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mMovieCursor = cursor;
		
        mAdapter = new MovieAdapter(this, cursor);
        mList.setAdapter(mAdapter);
		
		mLength = cursor.getCount();
		invalidateOptionsMenu();
		
		if(mLength == 0) {
			mEmptyMessage.setVisibility(View.VISIBLE);
			long bucketId = getIntent().getLongExtra(EXTRA_MOVIE_BUCKET, 0L);
			if(bucketId != 0) {
				finish();
			}
		} else {
			mEmptyMessage.setVisibility(View.GONE);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
        mAdapter = new MovieAdapter(this, mMovieCursor);
        mList.setAdapter(mAdapter);
	}

}
