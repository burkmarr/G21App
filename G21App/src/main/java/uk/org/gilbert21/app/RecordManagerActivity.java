package uk.org.gilbert21.app;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.OSRef;

public class RecordManagerActivity extends ListActivity {

    private ImageView mIconIv = null;
    private Boolean mPlaying = false;
    private MediaPlayer mPlayer = null;
    private Integer mPosition = 0;
    private Context context = this;
    private String mFolderFiles = "";
    private int mFileDeleteIndex;
    //private ArrayAdapter<SoundFile> mArrayAdapter;
    private SoundFileArrayAdapter mArrayAdapter;
    private ArrayList<SoundFile> mSoundFiles;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setTitle(getString(R.string.manage_recordings));

        //Set the media stream
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mFolderFiles = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFolderFiles += "/Android/data/";
        mFolderFiles += getApplicationContext().getPackageName();
        mFolderFiles += "/files";
        File file = new File(mFolderFiles);
        mSoundFiles = new ArrayList<SoundFile>();

        if (!file.exists()) {
            Toast.makeText(getApplicationContext(), "There are no recordings to display.", Toast.LENGTH_LONG).show();
        } else {
            File[] list = file.listFiles();
            for( int i=0; i< list.length; i++)
            {
                if (list[i].getName().endsWith(".3gp") || list[i].getName().endsWith(".wav")) {
                    SoundFile sf = new SoundFile(list[i]);
                    mSoundFiles.add(sf);
                }
            }
        }

        Collections.sort(mSoundFiles, new Comparator<SoundFile>() {
            public int compare(SoundFile sf1, SoundFile sf2) {
                //return obj1.name.compareToIgnoreCase(obj2.name);
                if (sf1.getFile().lastModified() > sf2.getFile().lastModified()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        // Binding resources Array to ListAdapter
        //mArrayAdapter = new ArrayAdapter<SoundFile>(this, R.layout.file_list_item, R.id.file_item_text, mSoundFiles);
        mArrayAdapter = new SoundFileArrayAdapter(this, R.layout.file_list_item, mSoundFiles);
        this.setListAdapter(mArrayAdapter);

        ListView lv = getListView();

        // Longclick for a delete
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int arg2, long arg3) {

                LinearLayout layout = (LinearLayout) view;
                //((TextView) layout.getChildAt(2)).getText().toString();
                mFileDeleteIndex = arg2;

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                AlertDialog show = builder
                        .setTitle(getString(R.string.dlg_title_delete_recording))
                        .setMessage(getString(R.string.dlg_question_delete_recording))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(getString(R.string.dlg_delete_recording_yes), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                //Delete the file
                                File file = mSoundFiles.get(mFileDeleteIndex).getFile();
                                try {
                                    file.delete();
                                } catch (Exception e) {
                                    Toast.makeText(context, mFileDeleteIndex + "Can't delete file " + file.getName(), Toast.LENGTH_LONG).show();
                                }
                                //mSoundFileLabels.remove(mFileDeleteIndex);
                                mSoundFiles.remove(mFileDeleteIndex);
                                mArrayAdapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton(getString(R.string.dlg_delete_recording_no), null)
                        .show();

                return false;
            }
        });

        // listening to single list item on click
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                if (mPlaying && position==mPosition) {
                    //Stop the current playback
                    mPlayer.stop();
                    mIconIv.setImageDrawable(getResources().getDrawable(R.drawable.playbackgreen));
                    mPlaying = false;
                } else if (!mPlaying) {

                    LinearLayout layout = (LinearLayout) view;
                    //Selected file
                    //String fileSelected = ((TextView) layout.getChildAt(1)).getText().toString();
                    String fileSelected = mSoundFiles.get(position).getFilePath();
                    //Icon image view
                    mIconIv = (ImageView) layout.getChildAt(0);

                    //Toast.makeText( getApplicationContext(), fileSelected, Toast.LENGTH_LONG).show();

                    mPlayer = new MediaPlayer();
                    mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer player) {
                            mIconIv.setImageDrawable(getResources().getDrawable(R.drawable.playbackgreen));
                            mPlaying = false;
                        }
                    });
                    try {
                        mIconIv.setImageDrawable(getResources().getDrawable(R.drawable.playbackred));
                        mPlayer.setDataSource(getApplicationContext(), Uri.parse(fileSelected));
                        mPlayer.prepare();
                        mPlayer.start();
                        mPlaying = true;
                    } catch (IOException e) {
                        mIconIv.setImageDrawable(getResources().getDrawable(R.drawable.playbackgreen));
                        Toast.makeText(getApplicationContext(), getString(R.string.cant_replay) + fileSelected, Toast.LENGTH_LONG).show();
                        mPlaying = false;
                    }
                    mPosition = position;
                }
            }
        });
    }
}
