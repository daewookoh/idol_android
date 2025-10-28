//
// Created by Booyoung Park on 2018. 7. 27..
//

#include <jni.h>

JNIEXPORT jstring JNICALL
 Java_net_ib_mn_billing_util_onestore_AppSecurity_getPublicKey(JNIEnv *env, jobject instance)
 {
 return (*env)->NewStringUTF(env, "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCdqcx17KoQNCmQu27Ruof1WRau0wokMPOmUxIVIEvSXCEQ1PV7J6F58VanDRl3zqqJwOIHLt2wzywd2CLzrS1FaAetNKJgu+1Rbc89q1xRFBfB7vvw/ijjCgPByqoln3g3qtvZnRA+2K9+qQqVd6X5l75REvT4F/UJh9+WQPBMCwIDAQAB");
}