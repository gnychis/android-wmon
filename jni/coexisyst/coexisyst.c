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
	struct usb_bus *bus, *head;
  	jobjectArray names = 0;
	jstring      str;
  	jsize        len = 0;
	int          i=0;
  
	// Get the busses and save the head
  	usb_find_busses();
	usb_find_devices();
	head = usb_busses;
	
	// Find the number of devices
	bus = head;
	for (bus = usb_busses; bus; bus = bus->next)
		if (bus->root_dev)
			len++;
  
  	// Create an array for the devices
	names = (*env)->NewObjectArray(env, len, (*env)->FindClass(env, "java/lang/String"), 0);

	// Loop through and get all of the devices
	bus = head;
	for (bus = usb_busses; bus; bus = bus->next) {
		if (bus->root_dev) { 	
			char name_buf[512];
			get_device_name(bus->root_dev, 0, name_buf);
			str = (*env)->NewStringUTF( env, name_buf );
			(*env)->SetObjectArrayElement(env, names, i, str);
		}
	}	
	
	return names;
}