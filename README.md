# NfcReactNative

nfc-react-native is a react-native module for android to write/read Mifare Classic (NFC) tags.

## Getting started

`$ npm install nfc-react-native --save`

### Mostly automatic installation

`$ react-native link nfc-react-native`

### Manual installation

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import es.tiarg.nfcreactnative.NfcReactNativePackage;` to the imports at the top of the file
  - Add `new NfcReactNativePackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':nfc-react-native'
  	project(':nfc-react-native').projectDir = new File(rootProject.projectDir, 	'../node_modules/nfc-react-native/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':nfc-react-native')
  	```

## Usage
```javascript
import {readTag, writeTag} from 'nfc-react-native'

...
  componentWillMount() {  
  			 //content                          sector block key
    writeTag('12341234123412341234123412341234', 1, 0, 'FFFFFFFFFFFF').then((r) => {
																	       console.log('from promise')
																	       console.log(r)
			//sector block key														   }) 
    readTag(1, 0, 'FFFFFFFFFFFF').then((r) => {
	                                  console.log('from promise')
	                                  console.log(r)
                                 })
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

Add a xml file in res folder with the following:
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