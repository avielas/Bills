LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

ifdef OPENCV_ANDROID_SDK
  ifneq ("","$(wildcard $(OPENCV_ANDROID_SDK)/OpenCV.mk)")
    include /home/aviel/Downloads/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk
  else
    include /home/aviel/Downloads/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk
  endif
else
  include /home/aviel/Downloads/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk
endif

LOCAL_MODULE    := mixed_sample
LOCAL_SRC_FILES := jni_part.cpp
LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)
