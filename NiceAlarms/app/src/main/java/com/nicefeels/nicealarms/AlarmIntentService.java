package com.nicefeels.nicealarms;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by simon on 13/09/2015.
 */
public class AlarmIntentService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public AlarmIntentService(String name) {
        super(name);
    }

    public AlarmIntentService() {
        super("AlarmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }
}
