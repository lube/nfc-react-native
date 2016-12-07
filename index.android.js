import React from 'react-native';

const NfcReactNative = React.NativeModules.NfcReactNative;

export const readTag = NfcReactNative.readTag;
export const writeTag = NfcReactNative.writeTag;
export const getCardId = NfcReactNative.getCardId;
