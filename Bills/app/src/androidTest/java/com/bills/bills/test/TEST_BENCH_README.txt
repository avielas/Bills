/********************************************************************************************/
/*************** This is a TEST_README.txt file for BS (c) 2016-forever. ********************/
/********************************************************************************************/

INSTRUCTIONS

-. open adb shell
   *. make sure your emulator is running
   *. open cmd
   *. go to sdk\platform-tools
   *. write'sudo ./adb shell'
   *. write 'su' to became sudo

-. changing of emulator Build.BRAND and Build.MODEL:
   *. changing to root: 'sudo ./adb root'
   *. ./adb pull /system/build.prop "/home/aviel/Desktop/BS"
   *. changing the following two properties (for example for emulator Nexus_6P_API_26) by gedit
        ro.product.model=6P_API_26
        ro.product.brand=Nexus
   *. before push build.prop back to emulator, we should prepare emulator to be writable
      - go to ~/Android/Sdk/emulator
      - see all emulators: ./emulator -list-avds
      - run emulator with write privilage: sudo ./emulator -writable-system -avd Nexus_6P_API_26 -no-snapshot-load -qemu
      - go to ~/Android/Sdk/platform-tools
      - sudo ./adb shell
      - su
      - mount
      - mount -o rw,remount -t /system
   *. now you can push file back: ./adb push "/home/aviel/Desktop/BS/build.prop" /system
   *. now rerun the emulator as mentioned before

-. copy files from pc to emulator
   *. sudo ./adb shell
   *. su
   *. ./adb push ~/Desktop/BS/TesseractSample /data/local/tmp

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
       ./adb pull /data/local/tmp/TesseractSample/TestsOutput.txt /data/local/tmp/TesseractSample/FailedTestsOutput.txt  ~/Desktop/BS

-. How to use tests?
    *. read Tests Configuration at the begin of OCRWrapperTest.java