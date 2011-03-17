/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <string.h>
#include <jni.h>
#include <usb.h>

/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   apps/samples/hello-jni/project/src/com/example/HelloJni/HelloJni.java
 */
jstring
Java_com_gnychis_coexisyst_CoexiSyst_initUSB( JNIEnv* env, jobject thiz )
{
	usb_init();
    return (*env)->NewStringUTF(env, "CoexiSyst system library and USB enabled...");
}

jobjectArray
Java_com_gnychis_coexisyst_CoexiSyst_getDeviceNames( JNIEnv* env, jobject thiz )
{
	jobjectArray ret;

	usb_find_busses();
	usb_find_devices();
    int i;

    char *message[5]= {"first", 
	"second", 
	"third", 
	"fourth", 
	"fifth"};

    ret= (jobjectArray)(*env)->NewObjectArray(5,
         (*env)->FindClass("java/lang/String"),
         (*env)->NewStringUTF(""));

    for(i=0;i<5;i++) {
        env->SetObjectArrayElement(
		ret,i,env->NewStringUTF(env, message[i]));
    }

	return (ret);
}