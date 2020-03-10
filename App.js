/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 */

import React from 'react';
import {View, Button} from 'react-native';

import FP from './services/FingerprintModule';

const App = () => {
  const handleTest = () => {
    FP.open(
      () => {
        console.log('success');
      },
      () => {
        console.log('error');
      },
    );
  };

  return (
    <View
      style={{
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
      }}>
      <Button onPress={handleTest} title="Test" />
    </View>
  );
};

export default App;
