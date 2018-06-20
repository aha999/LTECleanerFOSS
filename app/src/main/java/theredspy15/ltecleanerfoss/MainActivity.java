package theredspy15.ltecleanerfoss;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sdsmdg.tastytoast.TastyToast;

import net.alhazmy13.catcho.library.Catcho;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import in.codeshuffle.typewriterview.TypeWriterView;

public class MainActivity extends AppCompatActivity {

    List<String> whiteListedPaths = new ArrayList<>();
    List<String> extensionFilter = new ArrayList<>();

    List<File> foundFiles;
    int amountRemoved = 0;

    TypeWriterView typeWriterView;
    LinearLayout fileListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // catches app crashes and creates error reports
        Catcho.Builder(this)
                .recipients("hjadar15@protonmail.com")
                .build();

        typeWriterView = findViewById(R.id.typeWriterView);
        fileListView = findViewById(R.id.fileListView);

        setUpTypeWriter();
        setUpWhiteListAndFilter();
        requestWriteExternalPermission();
    }

    /**
     * Runs search and delete on background thread
     */
    public final void clean(View view) {

        new Thread(this::searchAndDeleteFiles).start();
    }

    /**
     * Searches entire device, adds all files to a list, then a for each loop filters
     * out files for deletion. Repeats the process as long as it keeps finding files to clean,
     * unless nothing is found to begin with
     */
    private void searchAndDeleteFiles() {

        Looper.prepare();

        amountRemoved = 0;
        byte cycles = 3;

        // removes the need to 'clean' multiple times to get everything
        for (int i = 0; i < cycles; i++) {

            // forward slash for whole device
            String path = Environment.getExternalStorageDirectory().toString() + "/";
            File directory = new File(path);
            foundFiles = getListFiles(directory);

            for (File file : foundFiles)
                if (checkExtension(file))
                    deleteFile(file);

            // No (more) files found
            if (amountRemoved == 0) break;
            else ++cycles;

            amountRemoved = 0;
        }

        Looper.loop();
    }

    /**
     * Used to generate a list of all files on device
     * @param parentDirectory where to start searching from
     * @return List of all files on device (besides whitelisted ones)
     */
    private List<File> getListFiles(File parentDirectory) {

        ArrayList<File> inFiles = new ArrayList<>();
        File[] files = parentDirectory.listFiles();

        for (File file : files) {
            if (!isPathWhiteListed(file)) // won't touch if whitelisted
                if (file.isDirectory()) { // folder
                    if (isDirectoryEmpty(file)) deleteFile(file); // delete if empty
                    else inFiles.addAll(getListFiles(file)); // add contents to returned list
                } else inFiles.add(file); // file
        }

        return inFiles;
    }

    /**
     * lists the contents of the file to an array, if the array length is 0, then return true,
     * else false
     * @param directory directory to test
     * @return true if empty, false if containing a file(s)
     */
    private boolean isDirectoryEmpty(File directory) {

        String[] files = directory.list();
        return files.length == 0;
    }

    /**
     * Increments amount removed, then creates a text view to add to the scroll view.
     * If there is any error while deleting, creates toast
     * @param file file to delete
     */
    private void deleteFile(File file) {

        // creating and adding a text view to the scroll view with path to file
        ++amountRemoved;
        TextView textView = new TextView(MainActivity.this);
        textView.setTextColor(Color.WHITE);
        textView.setText(file.getAbsolutePath());

        // adding to scroll view
        runOnUiThread(() -> fileListView.addView(textView));

        // deletion & error message
        String errorMessage = getResources().getString(R.string.error_when_deleting);
        errorMessage = errorMessage.concat(" " + file.getName());
        if (!file.delete()) TastyToast.makeText(
                MainActivity.this, errorMessage, TastyToast.LENGTH_LONG, TastyToast.ERROR
        ).show();
    }

    /**
     * Runs a for each loop through the white list, and compares the path of the file
     * to each path in the list
     * @param file file to check
     * @return true if is the file is in the white list, false if not
     */
    private boolean isPathWhiteListed(File file) {

        for (String path : whiteListedPaths) if (path.equals(file.getAbsolutePath())) return true;

        return false;
    }

    /**
     * Sets up the type writer style text view
     */
    private void setUpTypeWriter() {

        String text = getResources().getString(R.string.lte_cleaner);
        typeWriterView.setDelay(1);
        typeWriterView.setWithMusic(false);
        typeWriterView.animateText(text);
    }

    /**
     * Runs as for each loop through the extension filter, and checks if
     * the file name contains the extension
     * @param file file to check
     * @return true if the file's extension is in the filter, false otherwise
     */
    private boolean checkExtension(File file) {

        for (String extension : extensionFilter) if (file.getName().contains(extension)) return true;

        return false;
    }

    /**
     * Adds paths to the white list that are not to be cleaned. As well as adds
     * extensions to filter
     */
    private void setUpWhiteListAndFilter() {

        // white list
        whiteListedPaths.add("/storage/emulated/0/Music");
        whiteListedPaths.add("/storage/emulated/0/Podcasts");
        whiteListedPaths.add("/storage/emulated/0/Ringtones");
        whiteListedPaths.add("/storage/emulated/0/Alarms");
        whiteListedPaths.add("/storage/emulated/0/Notifications");
        whiteListedPaths.add("/storage/emulated/0/Pictures");
        whiteListedPaths.add("/storage/emulated/0/Movies");
        whiteListedPaths.add("/storage/emulated/0/Download");
        whiteListedPaths.add("/storage/emulated/0/DCIM");
        whiteListedPaths.add("/storage/emulated/0/Documents");

        // filter
        extensionFilter.add(".tmp");
        extensionFilter.add(".log");
        extensionFilter.add(".cache");
    }

    /**
     * Request write permission
     */
    public void requestWriteExternalPermission() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1);
    }

    /**
     * Handles the whether the user grants permission. Closes app on deny
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 // Granted
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) break;
                else System.exit(0); // Permission denied
                break;
        }
    }
}