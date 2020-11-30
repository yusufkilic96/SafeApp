package com.example.safeapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.widget.Toast.makeText;


public class VoiceActivity extends Activity implements RecognitionListener {

    private TextView txvResult;

    private String LOG_TAG = "VoiceRecognitionActivity";

    RecyclerView recyclerView;

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private SpeechRecognizer recognizer;

    private static final String KWS_SEARCH = "wakeup";
    private static final String MENU_SEARCH = "menu";
    private static final String CALL_MOM_SEARCH = "call mom";
    private static final String CALL_DAD_SEARCH = "call dad";
    private static final String CALL_BROTHER_SEARCH = "call brother";


    private static final String KEYPHRASE = "hello world";
    private static final String KEYPHRASE2 = "call mom";
    private static final String KEYPHRASE3 = "call dad";
    private static final String KEYPHRASE4 = "call brother";

    private static String tel_number = "05394961807";



    private HashMap<String, Integer> captions;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);
        txvResult = (TextView) findViewById(R.id.txvResult);

        captions = new HashMap<>();
        captions.put(KWS_SEARCH, R.string.kws_caption);
        captions.put(MENU_SEARCH, R.string.menu_caption);
        captions.put(CALL_MOM_SEARCH, R.string.call_mom_caption);
        captions.put(CALL_DAD_SEARCH, R.string.call_dad_caption);
        captions.put(CALL_BROTHER_SEARCH, R.string.call_brother_caption);
        ((TextView) findViewById(R.id.txvResult))
                .setText("Preparing the recognizer");

        recyclerView = findViewById(R.id.recyclerView);

        String[] names = {"mom", "dad", "brother"};
        String[] numbers = {"100", "05123123123123", "05123123123123"};

        MyAdapter myAdapter = new MyAdapter(this, names, numbers );


        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }

        recyclerView.setAdapter(myAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        new SetupTask(this).execute();
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE))
            switchSearch(MENU_SEARCH);
        else if (text.equals(CALL_MOM_SEARCH)) {
            switchSearch(CALL_MOM_SEARCH);
            tel_number="100";
            onCall();
        }
        else if (text.equals(CALL_DAD_SEARCH)) {
            switchSearch(CALL_DAD_SEARCH);
            tel_number="05123123123123";
            onCall();
        }
        else if (text.equals(CALL_BROTHER_SEARCH)) {
            switchSearch(CALL_BROTHER_SEARCH);
            tel_number="05123123123123";
            onCall();
        }


        else
            ((TextView) findViewById(R.id.result_text)).setText(text);
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        ((TextView) findViewById(R.id.result_text)).setText("");
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onError(Exception e) {
        ((TextView) findViewById(R.id.txvResult)).setText(e.getMessage());
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }

    private static class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<VoiceActivity> activityReference;
        SetupTask(VoiceActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }
        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                activityReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {
                ((TextView) activityReference.get().findViewById(R.id.txvResult))
                        .setText("Failed to init recognizer " + result);
            } else {
                activityReference.get().switchSearch(KWS_SEARCH);
            }
        }
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                .getRecognizer();
        recognizer.addListener(this);

        /* In your application you might not need to add all those searches.
          They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        recognizer.addKeyphraseSearch(CALL_MOM_SEARCH, KEYPHRASE2);
        recognizer.addKeyphraseSearch(CALL_DAD_SEARCH, KEYPHRASE3);
        recognizer.addKeyphraseSearch(CALL_BROTHER_SEARCH, KEYPHRASE4);



        // Create grammar-based search for selection between demos
        File menuGrammar = new File(assetsDir, "menu.gram");
        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);


    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);

        String caption = getResources().getString(captions.get(searchName));
        ((TextView) findViewById(R.id.txvResult)).setText(caption);
    }

    public void onCall() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    Integer.parseInt("123"));
        } else {
            startActivity(new Intent(Intent.ACTION_CALL).setData(Uri.parse("tel:" + tel_number)));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {

            case 123:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    onCall();
                } else {
                    Log.d("TAG", "Call Permission Not Granted");
                }
                break;

            default:
                break;
        }
    }

}
