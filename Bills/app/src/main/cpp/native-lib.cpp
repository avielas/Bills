#include <jni.h>
#include <string>

extern "C"
jstring
Java_com_bills_bills_BillsMainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
