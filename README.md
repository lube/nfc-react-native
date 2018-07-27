# NfcReactNative

nfc-react-native is a react-native module for android to write/read Mifare Classic (NFC) tags.

## Getting started

`$ npm install nfc-react-native --save`

### Mostly automatic installation

`$ react-native link nfc-react-native`

### Manual installation

#### Android

1. Open up `android/app/src/main/java/[...]/MainApplication.java`
  - Add `import es.tiarg.nfcreactnative.NfcReactNativePackage;` to the imports at the top of the file
  - Add `new NfcReactNativePackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
    ```
    include ':nfc-react-native'
    project(':nfc-react-native').projectDir = new File(rootProject.projectDir,  '../node_modules/nfc-react-native/android')
    ```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
    ```
      compile project(':nfc-react-native')
    ```

## Usage
```javascript
import { getTagId, readTag, writeTag } from 'nfc-react-native'

...
export default class NfcSample extends Component {
  readTagId() {
    getTagId()
  }

  readTagData() {
    readTag([
      { sector: 1, blocks: [1,2], clave: 'FFFFFFFFFFFF', keyType: 'A' },
      { sector: 2, blocks: [0,1,2], clave: 'FFFFFFFFFFFF', keyType: 'A' },
      { sector: 3, blocks: [0], clave: 'FFFFFFFFFFFF', keyType: 'A' }
    ])
  }

  writeTagData() {
    writeTag([{ sector: 1, blocks: [ 
    { index: 1, data: [15,15,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,15,15] },
    { index: 2, data: [15,15,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,15,15] } ],
      clave: 'FFFFFFFFFFFF', keyType: 'A' },
      { sector: 2, blocks: [ 
    { index: 0, data: [15,15,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,15,15] },
    { index: 1, data: [15,15,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,15,15] },
    { index: 2, data: [15,15,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,15,15] } ],
      clave: 'FFFFFFFFFFFF', keyType: 'A' },
    { sector: 3, blocks: [ 
    { index: 0, data: [15,15,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,15,15] } ],
      clave: 'FFFFFFFFFFFF', keyType: 'A' },
      ], 1148002313)
  }

  componentDidMount() {
    DeviceEventEmitter.addListener('onTagError', function (e) {
        console.log('error', e)
        Alert.alert(JSON.stringify(e))
    })

    DeviceEventEmitter.addListener('onTagDetected', function (e) {
        Alert.alert(JSON.stringify(e))
    })

    DeviceEventEmitter.addListener('onTagRead', (e) => {
        console.log('reading', e)
        Alert.alert(JSON.stringify(e))
    })

    DeviceEventEmitter.addListener('onTagWrite', (e) => {
        console.log('writing', e)
        Alert.alert(JSON.stringify(e))
    })
  }

  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          Welcome to React Native!
        </Text>
        <Button
          onPress={this.readTagId}
          title="Get id of Tag"
        />
        <Button
          onPress={this.readTagData}
          title="Get sectors of a Tag"
        />
        <Button
          onPress={this.writeTagData}
          title="Write sectors of a Tag"
        />
      </View>
    );
  }
}
...
```

## Configuration

In your manifest add:
```xml
<uses-permission android:name="android.permission.NFC" />
....
```
Your main activity should look like
```xml
...
<activity
  android:name=".MainActivity"
  android:launchMode="singleTask"
  android:label="@string/app_name"
  android:configChanges="keyboard|keyboardHidden|orientation|screenSize">

  <intent-filter>
    <action android:name="android.nfc.action.TECH_DISCOVERED"/>
  </intent-filter>

  <meta-data android:name="android.nfc.action.TECH_DISCOVERED"
             android:resource="@xml/nfc_tech_filter" />
</activity>
```

Add a xml file in `android/app/src/main/res` folder (create if it doesn't exist) with the following:
```xml
<resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
  <tech-list>
    <tech>android.nfc.tech.MifareClassic</tech>
  </tech-list>
</resources>
```

## Contribution
Contributions are welcome :raised_hands:

## License
This repository is distributed under [MIT license](https://github.com/Lube/nfc-react-native/blob/master/LICENSE) 
