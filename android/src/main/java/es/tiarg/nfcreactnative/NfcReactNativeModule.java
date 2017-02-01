package es.tiarg.nfcreactnative;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static android.R.attr.action;
import static android.R.attr.data;
import static android.R.attr.defaultValue;
import static android.R.attr.id;
import static android.R.attr.tag;
import static android.R.attr.x;
import static android.content.ContentValues.TAG;
import static android.view.View.X;
import static com.facebook.common.util.Hex.hexStringToByteArray;



class NfcReactNativeModule extends ReactContextBaseJavaModule implements ActivityEventListener, LifecycleEventListener {
    private ReactApplicationContext reactContext;

    private String operation;
    private int tagId;

    private ReadableArray sectores;
    private NfcAdapter mNfcAdapter;
    private MifareClassic tag;

    private static final String OP_ID = "ID";
    private static final String OP_WRITE = "WRITE";
    private static final String OP_READ = "READ";
    private static final String OP_NOT_READY = "NOT_READY";

    private class ThreadLectura implements Runnable {
        public void run() {
            if (tag != null && operation != OP_NOT_READY) {
                try {
                    tag.connect();

                    ByteBuffer bb = ByteBuffer.wrap(tag.getTag().getId());
                    int id = bb.getInt();

                    if (operation.equals(OP_ID)) {
                        WritableMap idData = Arguments.createMap();
                        idData.putInt("id", id);
                        reactContext
                                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                .emit("onTagDetected", idData);
                        return;
                    }

                    WritableMap readData = Arguments.createMap();
                    WritableArray writeData = Arguments.createArray();
                    WritableArray readDataSectors = Arguments.createArray();
                    readData.putInt("tagId", id);

                    for (int i = 0; i < sectores.size(); i++)
                    {
                        boolean authResult;
                        if (sectores.getMap(i).getString("keyType").equals("A")) {
                            authResult = tag.authenticateSectorWithKeyA(sectores.getMap(i).getInt("sector"), hexStringToByteArray(sectores.getMap(i).getString("clave")));
                        } else {
                            authResult = tag.authenticateSectorWithKeyB(sectores.getMap(i).getInt("sector"), hexStringToByteArray(sectores.getMap(i).getString("clave")));
                        }

                        if (tagId != 0 && operation.equals(OP_WRITE) && tagId != id) {
                            WritableMap error = Arguments.createMap();
                            error.putString("error", "Tag id doesn't match");

                            reactContext
                                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                    .emit("onTagError", error);

                            operation = OP_NOT_READY;
                            return;
                        }
                        
                        if (authResult) {
                            
                            WritableMap dataSector = Arguments.createMap();
                            WritableArray blocksXSector = Arguments.createArray();
                            
                            switch (operation) {
                                case OP_READ:
                                    for (int j = 0; j < sectores.getMap(i).getArray("blocks").size(); j++) 
                                    {
                                        int iBloque = sectores.getMap(i).getArray("blocks").getInt(j);
                                        byte[] blockData = tag.readBlock(4 * sectores.getMap(i).getInt("sector") + iBloque);
                                        blocksXSector.pushArray(Arguments.fromArray(arrayBytesToArrayInts(blockData)));
                                    }

                                    dataSector.putArray("blocks", blocksXSector);
                                    dataSector.putInt("sector", sectores.getMap(i).getInt("sector"));

                                    readDataSectors.pushMap(dataSector);
                                    break;

                                case OP_WRITE:
                                    for (int k = 0; k < sectores.getMap(i).getArray("blocks").size(); k++)
                                    {
                                        ReadableMap rmBloque = sectores.getMap(i).getArray("blocks").getMap(k);

                                        ReadableNativeArray data = (ReadableNativeArray)rmBloque.getArray("data");

                                        int[] writeDataA = new int[data.size()];
                                        for(int l = 0; l < data.size(); l++)
                                            writeDataA[l] = data.getInt(l);

                                        int blockIndex = 4 * sectores.getMap(i).getInt("sector") + rmBloque.getInt("index");
                                        tag.writeBlock(blockIndex, arrayIntsToArrayBytes(writeDataA));

                                        blocksXSector.pushMap(Arguments.createMap());
                                    }
                                    dataSector.putArray("blocks", blocksXSector);
                                    dataSector.putInt("sector", sectores.getMap(i).getInt("sector"));
                                    writeData.pushMap(dataSector);
                                    break;

                                default:
                                    break;
                            }
                        }
                        else {
                            WritableMap error = Arguments.createMap();
                            error.putString("error", "Auth error");

                            reactContext
                                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                    .emit("onTagDetected", error);

                            operation = OP_NOT_READY;
                            return;
                        }
                    }

                    tag.close();

                    readData.putArray("lectura",readDataSectors);
                    if (operation.equals(OP_READ)) {
                        reactContext
                                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                .emit("onTagRead", readData);
                    }

                    if (operation.equals(OP_WRITE)){
                        reactContext
                                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                .emit("onTagWrite", writeData);
                    }

                    tag.close();
                } catch (Exception ex) {
                    WritableMap error = Arguments.createMap();
                    error.putString("error", ex.toString());

                    reactContext
                            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                            .emit("onTagError", error);
                    tag = null;
                } finally {
                    operation = OP_NOT_READY;
                }
            }
        }
    }

    public NfcReactNativeModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.tag = null;

        this.reactContext.addActivityEventListener(this);
        this.reactContext.addLifecycleEventListener(this);

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new ThreadLectura(), 0, 5, TimeUnit.SECONDS);

        this.operation = OP_NOT_READY;
    }

    @Override
    public void onHostResume() {
        if (mNfcAdapter != null) {
            setupForegroundDispatch(getCurrentActivity(), mNfcAdapter);
        } else {
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this.reactContext);
        }
    }

    @Override
    public void onHostPause() {
        if (mNfcAdapter != null)
            stopForegroundDispatch(getCurrentActivity(), mNfcAdapter);
    }

    @Override
    public void onHostDestroy() {
        // Activity `onDestroy`
    }

    private void handleIntent(Intent intent) {
        this.tag = MifareClassic.get( (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
    }

    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
        adapter.enableForegroundDispatch(activity, pendingIntent, null, null);
    }

    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    @Override
    public void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    @Override
    public void onActivityResult(
            final Activity activity,
            final int requestCode,
            final int resultCode,
            final Intent intent) {
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
    public void readTag(ReadableArray sectores) {
        this.sectores = sectores;
        this.operation = OP_READ;
    }

    @ReactMethod
    public void writeTag(ReadableArray sectores,
                         int tagId) {
        this.tagId = tagId;
        this.sectores = sectores;
        this.operation = OP_WRITE;
    }

    @ReactMethod
    public void getTagId() {
        this.operation = OP_ID;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static String byteArrayToHexString(byte[] b) throws Exception {
      String result = "";
      for (int i=0; i < b.length; i++) {
        result +=
              Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
      }
      return result;
    }

    private static byte[] arrayIntsToArrayBytes(int[] listaInts) {
        
        ByteBuffer bytebuffer = ByteBuffer.allocate(16);
        for (int i = 0; i < 16; i++) {
            byte high = (byte)((byte)listaInts[i*2] & 0xf0 >> 4);
            byte low =  (byte)((byte)listaInts[i*2+1] & 0x0f);
            bytebuffer.put((byte)(high << 4 | low));
        }
        return bytebuffer.array();
    }

    private static int[] arrayBytesToArrayInts(byte[] listaBytes) {
        
        IntBuffer arraybuffer = IntBuffer.allocate(32);
        for(byte b : listaBytes) {
            int high = (b & 0xf0) >> 4;
            int low = b & 0x0f;
            arraybuffer.put(high);
            arraybuffer.put(low);
        }
        
        return arraybuffer.array();
    }

    static String bin2hex(byte[] data) {
        return String.format("%0" + (data.length * 2) + "X", new BigInteger(1,data));
    }
    
}
