package uk.org.gilbert21.app;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by richardb.ho on 30/01/2015.
 */

public class SoundFileArrayAdapter extends ArrayAdapter<SoundFile> {

    Context context;
    int layoutResourceId;
    //SoundFile data[] = null;
    ArrayList<SoundFile> data = null;

    public SoundFileArrayAdapter(Context context, int layoutResourceId, ArrayList<SoundFile> data){
    // public SoundFileArrayAdapter(Context context, int layoutResourceId, SoundFile[] data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        SoundFileHolder holder = null;

        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new SoundFileHolder();
            holder.imgIcon = (ImageView)row.findViewById(R.id.file_g21compat_image);
            holder.txtTitle = (TextView)row.findViewById(R.id.file_item_text);

            row.setTag(holder);
        }
        else
        {
            holder = (SoundFileHolder)row.getTag();
        }

        SoundFile soundFile = data.get(position);
        holder.txtTitle.setText(soundFile.toString());
        if (soundFile.isG21Compatible()) {
            holder.imgIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.g21compat));
        } else{
            //holder.imgIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.g21incompat));
            //Following update to G21 Desktop in Feb 2015, all three formats now compatible with desktop.
            holder.imgIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.g21compat));
        }

        return row;
    }

    static class SoundFileHolder
    {
        ImageView imgIcon;
        TextView txtTitle;
    }
}
