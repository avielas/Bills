/********************************************************************************************/
/*************** This is a TEST_README.txt file for BS (c) 2016-forever. ********************/
/********************************************************************************************/

INSTRUCTIONS

-. BEFORE RUNNING TEST YOU SHOULD COPY billsToTest FOLDER FROM ASSETS OF billsgenerator MODULE TO ASSETS OF
   CURRENT PROJECT AND ALSO REMOVE THE COMMENTED LINES ON THE BEGINNING OF THE TEST.
   NOW, THE TEST COPY THE IMAGES TO PHONE/EMULATOR BEFORE RUNNING

-. open adb shell
   *. make sure your emulator is running
   *. open cmd
   *. go to sdk\platform-tools
   *. write'sudo ./adb shell'
   *. write 'su' to became sudo

-. changing of emulator Build.BRAND and Build.MODEL:
   *. changing to root: 'sudo ./adb root'
   *. ./adb pull /system/build.prop "/home/aviel/Desktop/BS"
   *. changing the following two properties (for example for emulator Nexus_6_API_26) by gedit
        ro.product.model=6_API_26
        ro.product.brand=Nexus
   *. before push build.prop back to emulator, we should prepare emulator to be writable
      - go to ~/Android/Sdk/emulator
      - see all emulators: ./emulator -list-avds
      - run emulator with write privilage: sudo ./emulator -writable-system -avd Nexus_6_API_26_1 -no-snapshot-load -qemu
      - go to ~/Android/Sdk/platform-tools
      - sudo ./adb shell
      - su
      - mount
      - mount -o rw,remount -t /system
   *. now you can push file back: ./adb push "/home/aviel/Desktop/BS/build.prop" /system
   *. now rerun the emulator as mentioned before

-. copy files from pc to emulator
   *. adb push <pc-path> <emulator-path>
      for example:
      adb push
      "C:\Users\avielavr\Desktop\BS\billsplit\app\src\main\assets\tessdata\eng.traineddata"
      /sdcard/TesseractSample/tessdata

-. Project Directory
    *. cd /data/data/com.billsplit.billsplit/files/TesseractSample

-. Tests Report File
    *. the test creates it on:
       /data/data/com.billsplit.billsplit/files/TesseractSample/OCRWrapperTestOutput.txt
    *. copy to sdcard:
       -. see 1 to open adb shell as sudo
       -. cp /data/data/com.billsplit.billsplit/files/TesseractSample/OCRWrapperTestOutput.txt /sdcard/
          or
          cp /data/data/com.billsplit.billsplit/files/TesseractSample/imgs/ocr.jpg /sdcard/
       -. exit
       -. exit
    *. pull from sdcard to pc:
       adb pull /sdcard/OCRWrapperTestOutput.txt "C:\Users\avielavr\Desktop\BS"
       or
       adb pull /sdcard/ocr.jpg "C:\Users\avielavr\Desktop\BS"

-. How to use tests?
    *. read Tests Configuration at the begin of OCRWrapperTest.java