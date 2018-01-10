# Pinch 👌

Callback and promise based HTTP client that supports SSL pinning and upload files and text for React Native(ios and android).

## Installation

Using NPM:
```
npm install react-native-pinch
```

Using Yarn:
```
yarn add react-native-pinch
```

## Automatically link

#### With React Native 0.27+

```shell
react-native link react-native-pinch
```

#### With older versions of React Native

You need [`rnpm`](https://github.com/rnpm/rnpm) (`npm install -g rnpm`)

```shell
rnpm link react-native-pinch
```

## Manually link

### iOS (via Cocoa Pods)
Add the following line to your build targets in your `Podfile`

`pod 'RNPinch', :path => '../node_modules/react-native-pinch'`

Then run `pod install`

### Android

- in `android/app/build.gradle`:

```diff
dependencies {
    ...
    compile "com.facebook.react:react-native:+"  // From node_modules
+   compile project(':react-native-pinch')
}
```

- in `android/settings.gradle`:

```diff
...
include ':app'
+ include ':react-native-pinch'
+ project(':react-native-pinch').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-pinch/android')
```

#### With React Native 0.29+

- in `MainApplication.java`:

```diff
+ import com.localz.PinchPackage;

  public class MainApplication extends Application implements ReactApplication {
    //......

    @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
+         new PinchPackage(),
          new MainReactPackage()
      );
    }

    ......
  }
```

#### With older versions of React Native:

- in `MainActivity.java`:

```diff
+ import com.localz.PinchPackage;

  public class MainActivity extends ReactActivity {
    ......

    @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
+       new PinchPackage(),
        new MainReactPackage()
      );
    }
  }
```
## 覆盖 node_modules
下载github 文件，覆盖 node_modules下的 react-native-pinch文件夹
## Adding certificates

Before you can make requests using SSL pinning, you first need to add your `.cer` files to your project's assets.

### Android

 - Place your `.cer` files under `src/main/assets/`.

### iOS

 - Place your `.cer` files in your iOS Project. Don't forget to add them in your `Build Phases > Copy Bundle Resources`, in Xcode.


## Example
*Examples are using the ES6 standard*

Requests can be made by using the `fetch(url[, config, [callback]])` method of Pinch.
### upload file(s) and text for android and ios
```javascript
   var body = {
            title:'title'
        };
     pinch.fetch('http://api.nohttp.net/upload', {
            method: 'post',
            //headers:headers,
            body:JSON.stringify(body),
            upload: {
                files: ['/storage/emulated/0/Download/111314.png','/storage/emulated/0/Download/111313.png']
            },
            //params:{'title':'title','body':'body'},
            timeoutInterval: 10000, // timeout after 10 seconds
            sslPinning: {
                cert: 'sdfsd'
            }
        })
            .then(res => console.log(`We got your response! Response - ${res.bodyString})`))
            .catch(err => console.log(`Whoopsy doodle! Error - ${JSON.stringify(err)}`))
```
### Using Promises
```javascript
import pinch from 'react-native-pinch';

  var body = {
            username:"admin",
            password:"123456",
        };
pinch.fetch('https://my-api.com/v1/endpoint', {
  method: 'post',
  headers: { customHeader: 'customValue' },
  body:JSON.stringify(body),
  timeoutInterval: 10000, // timeout after 10 seconds
  sslPinning: {
    cert: 'cert-file-name', // cert file name without the `.cer`
    certs: ['cert-file-name-1', 'cert-file-name-2'], // optionally specify multiple certificates
  }
})
  .then(res => console.log(`We got your response! Response - ${res.bodyString}`))
  .catch(err => console.log(`Whoopsy doodle! Error - ${err}`))
```

### Using Callbacks
```javascript
import pinch from 'react-native-pinch';

pinch.fetch('https://my-api.com/v1/endpoint', {
  method: 'post',
  headers: { customHeader: 'customValue' },
  body: '{"firstName": "Jake", "lastName": "Moxey"}',
  timeoutInterval: 10000, // timeout after 10 seconds
  sslPinning: {
    cert: 'cert-file-name', // cert file name without the `.cer`
    certs: ['cert-file-name-1', 'cert-file-name-2'], // optionally specify multiple certificates
  }
}, (err, res) => {
  if (err) {
    console.error(`Whoopsy doodle! Error - ${err}`);
    return null;
  }
  console.log(`We got your response! Response - ${res.bodyString}`);
})
```

### Skipping validation

```javascript
import pinch from 'react-native-pinch';

pinch.fetch('https://my-api.com/v1/endpoint', {
  method: 'post',
  headers: { customHeader: 'customValue' },
  body: '{"firstName": "Jake", "lastName": "Moxey"}',
  timeoutInterval: 10000, // timeout after 10 seconds
  sslPinning: {} // omit the `cert` or `certs` key, `sslPinning` can be ommited as well
})
```

## Response Schema
```javascript
{
  bodyString: '',

  headers: {},

  status: 200,

  statusText: 'OK'
}
```
