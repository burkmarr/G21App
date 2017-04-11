package uk.org.gilbert21.app;

import android.text.format.DateFormat;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.OSRef;

/**
 * Created by richardb.ho on 30/01/2015.
 */
public class SoundFile {

    String mFilePath = null;
    String mGridRef = null;
    String mDate = null;
    String mTime = null;
    boolean bG21Compatible = false;
    File mFile = null;

    SoundFile(File file){

        mFile = file;
        mFilePath = file.getPath();
        String fileName = file.getName();
        fileName = fileName.substring(0, fileName.length()-4);
        String[] parts = fileName.split("_");

        if (parts.length == 4) {
            //Gilbert 21 format
            bG21Compatible = true;

            String strDateTime = parts[0];
            String strLat = parts[2];
            String strLon = parts[3];

            java.util.Date recDateTime = new java.util.Date(Long.parseLong(strDateTime));
            mDate = (DateFormat.format("dd/MM/yyyy", recDateTime)).toString();
            mTime = (DateFormat.format("kk:mm:ss", recDateTime)).toString();
            mGridRef = LatLon2GR(strLat, strLon);

        } else if (parts.length == 5) {
            bG21Compatible = false;

            //OSGR simple format
            String strDate = parts[0];
            String strTime = parts[1];
            mGridRef = parts[2];
            String strAcc = parts[3];
            String strAlt = parts[4];

            String[] splitDate = strDate.split("-");
            mDate = splitDate[2] + "/" + splitDate[1] + "/" + splitDate[0];
            mTime = strTime.replace("-", ":");

        } else { //parts == 6
            //Lat/lon simple format
            bG21Compatible = false;

            String strDate = parts[0];
            String strTime = parts[1];
            String strLat = parts[2];
            String strLon = parts[3];
            String strAcc = parts[4];
            String strAlt = parts[5];

            String[] splitDate = strDate.split("-");
            mDate = splitDate[2] + "/" + splitDate[1] + "/" + splitDate[0];
            mTime = strTime.replace("-", ":");
            mGridRef = LatLon2GR(strLat, strLon);
        }
    }

    File getFile() {

        return mFile;
    }

    String getFilePath() {

        return mFilePath;
    }

    String getDate() {

        return mDate;
    }

    String getTime() {

        return mTime;
    }

    String getGridRef() {

        return mGridRef;
    }

    Boolean isG21Compatible() {

        return bG21Compatible;
    }

    //getResources().getDrawable(R.drawable.g21incompat)

    private String doubleToString(double d, String pattern){

        DecimalFormat df =(DecimalFormat) NumberFormat.getInstance(Locale.getDefault());
        df.applyLocalizedPattern(pattern);

        return df.format(d);
    }

    private String LatLon2GR(String strLat, String strLon){

        LatLng latlng = new LatLng(Double.parseDouble(strLat), Double.parseDouble(strLon));
        latlng.toOSGB36();
        OSRef osRef = latlng.toOSRef();
        String osgr = osRef.toSixFigureString();
        String easting = doubleToString(osRef.getEasting(),"#0000000");
        String northing = doubleToString(osRef.getNorthing(),"#0000000");

        return osgr.substring(0,2) + easting.substring(2,6) + northing.substring(2,6);
    }

    @Override
    public String toString() {

        return mDate + " " + mTime + " " + mGridRef;
    }
}
