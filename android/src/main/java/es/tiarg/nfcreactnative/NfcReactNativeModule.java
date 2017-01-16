package es.tiarg.nfcreactnative;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import static android.R.attr.data;
import static android.R.attr.defaultValue;
import static android.R.attr.x;
import static com.facebook.common.util.Hex.hexStringToByteArray;

class NfcReactNativeModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private ReactApplicationContext reactContext;
    private byte[] key;
    private byte[] content;
    
    private int sector;
    private int block;
    private String operation;
    private int cardId;
    private ReadableArray sectores;
    private Promise tagPromise;

    private static final String OP_ID = "ID";
    private static final String OP_WRITE = "WRITE";
    private static final String OP_READ = "READ";
    private static final String OP_NOT_READY = "NOT_READY";

    public NfcReactNativeModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.reactContext.addActivityEventListener(this);
        this.operation = OP_NOT_READY;
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (this.operation.equals(OP_NOT_READY)) {
            return;
        }
        
        MifareClassic tag = MifareClassic.get( (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
        try {
            tag.connect();
            ByteBuffer bb = ByteBuffer.wrap(tag.getTag().getId());
            int id = bb.getInt();
            if (this.operation.equals(OP_ID)) {
                WritableMap idData = Arguments.createMap();
                idData.putInt("id", id);
                this.tagPromise.resolve(idData);
                return;
            }
            WritableMap readData = Arguments.createMap();
            WritableArray readDataSectors = Arguments.createArray();
            readData.putInt("card", id);
            WritableArray writeData = Arguments.createArray();
            for (int i = 0; i < this.sectores.size(); i++) {
                ReadableMap sector = this.sectores.getMap(i);
                boolean authResult;
                if (this.sectores.getMap(i).getString("tipoClave").equals("A")) {
                    authResult = tag.authenticateSectorWithKeyA(this.sectores.getMap(i).getInt("sector"), hexStringToByteArray(this.sectores.getMap(i).getString("clave")));
                } else {
                    authResult = tag.authenticateSectorWithKeyB(this.sectores.getMap(i).getInt("sector"), hexStringToByteArray(this.sectores.getMap(i).getString("clave")));
                }
                if (this.cardId == 0 && this.operation.equals(OP_WRITE) && this.cardId != id) {
                    this.tagPromise.reject("Id Error", "Apoye la misma tarjeta que leyo por primera vez");
                    return;
                }
                if (authResult) {
                    WritableMap dataSector = Arguments.createMap();
                    WritableArray bloquesXSector = Arguments.createArray();
                    switch (this.operation) {
                        case OP_READ:
                            for (int j = 0; j < this.sectores.getMap(i).getArray("bloques").size(); j++) {
                                int iBloque = this.sectores.getMap(i).getArray("bloques").getInt(j);
                                bloquesXSector.pushArray(Arguments.fromArray(arrayBytesToArrayInts(tag.readBlock(4 * this.sectores.getMap(i).getInt("sector") + iBloque))));
                            }
                            dataSector.putArray("bloques", bloquesXSector);
                            dataSector.putInt("sector", this.sectores.getMap(i).getInt("sector"));
                            readDataSectors.pushMap(dataSector);
                            break;
                        case OP_WRITE:
                            for (int k = 0; k < this.sectores.getMap(i).getArray("bloques").size(); k++) {
                                ReadableMap rmBloque = this.sectores.getMap(i).getArray("bloques").getMap(k);
                                ReadableNativeArray data = (ReadableNativeArray)rmBloque.getArray("data");
                                int[] writeDataA = new int[data.size()];
                                for(int l = 0; l < data.size(); l++)
                                    writeDataA[l] = data.getInt(l);
                                tag.writeBlock(4 * this.sectores.getMap(i).getInt("sector") + rmBloque.getInt("indice"), arrayIntsToArrayBytes(writeDataA));
                                bloquesXSector.pushMap(Arguments.createMap());
                            }
                            dataSector.putArray("bloques", bloquesXSector);
                            dataSector.putInt("sector", this.sectores.getMap(i).getInt("sector"));
                            writeData.pushMap(dataSector);
                            break;
                        case OP_NOT_READY:
                            return;
                        default:
                            break;
                    }
                }
                else {
                    this.tagPromise.reject("Auth Error", "Error de Autenticación con la tarjeta");
                    return;
                }
            }
            tag.close();
            readData.putArray("lectura",readDataSectors);
            if (this.operation.equals(OP_READ)) {
                this.tagPromise.resolve(readData);
            }
            if (this.operation.equals(OP_WRITE)){
                this.tagPromise.resolve(writeData);
            }
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            this.tagPromise.reject(sw.toString(), ex.getMessage());
        } finally {
            this.operation = OP_NOT_READY;
        }
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
    public void readTag(ReadableArray sectores,
                        Promise promise) {
        this.cancelOperation();
        this.sectores = sectores;
        this.operation = OP_READ;
        this.tagPromise = promise;
    }

    @ReactMethod
    public void writeTag(ReadableArray sectores,
                         int cardId,
                         Promise promise) {
        this.cancelOperation();
        this.cardId = cardId;
        this.sectores = sectores;
        this.operation = OP_WRITE;
        this.tagPromise = promise;
    }

    @ReactMethod
    public void getCardId(Promise promise) {
        this.cancelOperation();
        this.operation = OP_ID;
        this.tagPromise = promise;
    }

    private void cancelOperation () {
        if (!this.operation.equals(OP_NOT_READY)) {
            this.tagPromise.reject("Operacion Cancelada");
            this.operation = OP_NOT_READY;
        }
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
