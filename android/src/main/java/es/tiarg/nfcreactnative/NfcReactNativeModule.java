package es.tiarg.nfcreactnative;

import android.content.Context;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;

import javax.annotation.Nullable;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.util.SparseArray;

class NfcReactNativeModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private ReactApplicationContext reactContext;

    private byte[] key;
    private byte[] content;
    
    private int sector;
    private int block;

    private String operation;
    private Promise tagPromise;

    private static final String OP_WRITE = "WRITE";
    private static final String OP_READ = "READ";
    private static final String OP_NOT_READY = "NOT_READY";


    public NfcReactNativeModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.reactContext.addActivityEventListener(this);
    }

    private void sendEvent(ReactApplicationContext reactContext, 
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    /**
     * @return the name of this module. This will be the name used to {@code require()} this module
     * from javascript.
     */
    @Override
    public String getName() {
        return "NfcReactNative";
    }

    @ReactMethod
    public void readTag(int sector,
                        int block,
                        String key,
                        Promise promise) {
        this.operation = OP_READ;
        this.sector = sector;
        this.block = block;
        this.key = hexStringToByteArray(key);
        this.tagPromise = promise;
    }

    @ReactMethod
    public void writeTag(String content,
                         int sector,
                         int block,
                         String key,
                         Promise promise) {

        this.operation = OP_WRITE;
        this.content = hexStringToByteArray(content);
        this.sector = sector;
        this.block = block;
        this.key = hexStringToByteArray(key);
        this.tagPromise = promise;
    }

    @Override
    public void onNewIntent(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        WritableMap nfcData = Arguments.createMap();

        if (intent != null && NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareClassic mfc = MifareClassic.get(tagFromIntent);            

            try {
                mfc.connect();
                
                if (mfc.authenticateSectorWithKeyA(this.sector, this.key)) {
                    switch (this.operation) {
                        case OP_READ:
                            byte[] data = mfc.readBlock(4 * this.sector + this.block);
                              
                            nfcData.putString("data", byteArrayToHexString(data));
                            this.tagPromise.resolve(nfcData);
                            break;
                        case OP_WRITE:
                            mfc.writeBlock(4 * this.sector + this.block, this.content);
                              
                            nfcData.putString("data", byteArrayToHexString(this.content));
                            this.tagPromise.resolve(nfcData);
                            break;
                        case OP_NOT_READY:
                        default:
                            break;    
                    }
                } else {
                    this.tagPromise.reject("No se pudo autenticar con la tarjeta.");
                }

                mfc.close();
            } catch (Exception ex) {
            } finally {
                this.operation = OP_NOT_READY;
            }
        }
    }

    @Override
    public void onActivityResult(
      final Activity activity,
      final int requestCode,
      final int resultCode,
      final Intent intent) {
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String byteArrayToHexString(byte[] b) throws Exception {
      String result = "";
      for (int i=0; i < b.length; i++) {
        result +=
              Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
      }
      return result;
    }
}
    