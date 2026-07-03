package com.example.mobilnekt1;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public final class SlagalicaApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private int startedActivities;

    @Override public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    public boolean isInForeground() { return startedActivities > 0; }
    @Override public void onActivityStarted(Activity activity) { startedActivities++; }
    @Override public void onActivityStopped(Activity activity) { startedActivities = Math.max(0, startedActivities - 1); }
    @Override public void onActivityCreated(Activity activity, Bundle state) { }
    @Override public void onActivityResumed(Activity activity) { }
    @Override public void onActivityPaused(Activity activity) { }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
    @Override public void onActivityDestroyed(Activity activity) { }
}
