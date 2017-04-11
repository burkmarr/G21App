package uk.org.gilbert21.app;

/**
 * Created by richardb.ho on 24/01/2015.
 */
import android.location.Location;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GPX {

    private FileWriter mWriter = null;
    private File mFile = null;

    GPX(String filePath){

        mFile = new File(filePath);
        try{
            mWriter = new FileWriter(mFile, false);
        } catch (IOException e) {
            //Handle exception
        }

        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"MapSource 6.15.5\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\"><trk>\n";
        String name = "<name>G21 Track</name><trkseg>\n";

        try{
            mWriter.append(header);
            mWriter.append(name);
            mWriter.flush();
        } catch (IOException e) {
            //Handle exception
        }
    }

    public void addLoc(Location loc) {

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String vertice = "<trkpt lat=\"" + loc.getLatitude() + "\" lon=\"" + loc.getLongitude() + "\"><time>" + df.format(new Date(loc.getTime())) + "</time></trkpt>\n";
        try{
            mWriter.append(vertice);
            mWriter.flush();
        } catch (IOException e) {
            //Handle exception
        }
    }

    public void close(){

        String footer = "</trkseg></trk></gpx>";
        try{
            mWriter.append(footer);
            mWriter.flush();
            mWriter.close();
        } catch (IOException e) {
            //Handle exception
        }
    }

    public void delete() {

        mFile.delete();
    }
}
