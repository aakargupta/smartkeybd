package com.sonyericsson.extras.liveware.extension.keybdsample;


import com.sonyericsson.extras.liveware.extension.util.ExtensionService;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.registration.DeviceInfo;
import com.sonyericsson.extras.liveware.extension.util.registration.DisplayInfo;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationAdapter;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

import android.os.Handler;
import android.util.Log;

/**
 * The Sample Extension Service handles registration and keeps track of all
 * controls on all accessories.
 */
public class SampleExtensionService extends ExtensionService {

    public static final String EXTENSION_KEY = "com.sonyericsson.extras.liveware.extension.samplecontrol.key";

    public static final String LOG_TAG = "SampleControlExtension";

    public SampleExtensionService() {
        super(EXTENSION_KEY);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(SampleExtensionService.LOG_TAG, "SampleControlService: onCreate");
    }

    @Override
    protected RegistrationInformation getRegistrationInformation() {
        return new SampleRegistrationInformation(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sonyericsson.extras.liveware.aef.util.ExtensionService#
     * keepRunningWhenConnected()
     */
    @Override
    protected boolean keepRunningWhenConnected() {
        return false;
    }

    @Override
    public ControlExtension createControlExtension(String hostAppPackageName) {
        final int controlSWWidth = SampleControlSmartWatch.getSupportedControlWidth(this);
        final int controlSWHeight = SampleControlSmartWatch.getSupportedControlHeight(this);
        final int controlSWHPWidth = SampleControlSmartWirelessHeadsetPro
                .getSupportedControlWidth(this);
        final int controlSWHPHeight = SampleControlSmartWirelessHeadsetPro
                .getSupportedControlHeight(this);

        for (DeviceInfo device : RegistrationAdapter.getHostApplication(this, hostAppPackageName)
                .getDevices()) {
            for (DisplayInfo display : device.getDisplays()) {
                if (display.sizeEquals(controlSWWidth, controlSWHeight)) {
                    return new SampleControlSmartWatch(hostAppPackageName, this, new Handler());
                } else if (display.sizeEquals(controlSWHPWidth, controlSWHPHeight)) {
                    return new SampleControlSmartWirelessHeadsetPro(hostAppPackageName, this,
                            new Handler());
                }
            }
        }
        throw new IllegalArgumentException("No control for: " + hostAppPackageName);
    }
}
