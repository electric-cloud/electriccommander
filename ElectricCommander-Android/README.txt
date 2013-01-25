**Experimental purposes only**

ElectricCommander Android Application that logs into ElectricCommander and displays the last 20 jobs launched by the user.

Based off of the Android example: http://developer.android.com/training/basics/network-ops/index.html


A precompiled debug .apk is located in the bin directory.
CommanderAndroid-debug-unaligned.apk

How to compile it and install it on your device. 

You will need ant.
You will need the android sdk
You may need to set the ANDROID_HOME env variable to the location of the android sdk.


nvaze@NVAZE /c/Users/nvaze/Documents/GitHub/electriccommander/ElectricCommander-Android
$ /c/Users/nvaze/Downloads/apache-ant-1.8.4-bin/apache-ant-1.8.4/bin/ant debug

nvaze@NVAZE /c/Users/nvaze/Documents/GitHub/electriccommander/ElectricCommander-Android
/c/Users/nvaze/android-sdks/platform-tools/adb -d install bin/CommanderAndroid-debug.apk

Released under the Apache 2.0 License.

Copyright 2012 Electric Cloud, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.