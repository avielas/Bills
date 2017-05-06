/********************************************************************************************/
/*************** This is a TEST_README.txt file for BS (c) 2016-forever. ********************/
/********************************************************************************************/

INSTRUCTIONS

0. BEFORE RUNNING TEST YOU SHOULD COPY billsToTest FOLDER FROM ASSETS OF billsgenerator MODULE TO ASSETS OF
   CURRENT PROJECT AND ALSO REMOVE THE COMMENTED LINES ON THE BEGINNING OF THE TEST.
   NOW, THE TEST COPY THE IMAGES TO PHONE/EMULATOR BEFORE RUNNING

1. open adb shell
   *. make sure your emulator is running
   *. open cmd
   *. go to sdk\platform-tools
   *. write 'adb shell'
   *. write 'su' to became sudo

2. copy files from pc to emulator
   *. adb push <pc-path> <emulator-path>
      for example:
      adb push
      "C:\Users\avielavr\Desktop\BS\billsplit\app\src\main\assets\tessdata\eng.traineddata"
      /sdcard/TesseractSample/tessdata

3. Project Directory
    *. cd /data/data/com.billsplit.billsplit/files/TesseractSample

4. Tests Report File
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
5. How to use tests?
    *. read Tests Configuration at the begin of OCRWrapperTest.java