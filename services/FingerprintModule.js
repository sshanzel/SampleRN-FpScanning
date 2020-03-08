import {NativeModules} from 'react-native';

const FingerprintModule = NativeModules.FingerprintScan;

export default {
  open: FingerprintModule.openDevice,
  show: FingerprintModule.show,
  scan: FingerprintModule.scan,
  constants: {
    SHORT: FingerprintModule.SHORT,
  },
};
