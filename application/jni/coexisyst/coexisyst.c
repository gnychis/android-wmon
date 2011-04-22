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
#include <android/log.h>
#include "spectool_container.h" 
#include "spectool_net_client.h"
#include <errno.h>
#define LOG_TAG "CoexisystDriver" // text for log tag 

// For keeping track of the devices, made global to handle callbacks and still
// have the device information
static struct libusb_device_handle *devh = NULL;
wispy_device_list list;
wispy_phy *pi;
wispy_phy *devs = NULL;
int ndev = 0;
int *rangeset = NULL;
FILE *fh;
int sample = 0;


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

jint
Java_com_gnychis_coexisyst_CoexiSyst_initWiSpyDevices( JNIEnv* env, jobject thiz )
{
	int x;
	sample = 0;
	
	ndev = wispy_device_scan(&list);

	fh = fopen("/sdcard/coexisyst_raw.txt","w+");
	
	// Make sure that a device is connected
	if(ndev <= 0) {
		return 0;
	}

	if (ndev > 0) {
    	rangeset = (int *) malloc(sizeof(int) * ndev);
    	memset(rangeset, 0, sizeof(int) * ndev);
  	}
		
	// Initialize each of the devices
	for(x = 0; x < ndev; x++) {
			
		pi = (wispy_phy *) malloc(WISPY_PHY_SIZE);
		pi->next = devs;
		devs = pi;
		
		if(wispy_device_init(pi, &(list.list[x])) < 0) {
			return 0;
		}
		
		if(wispy_phy_open(pi) < 0) {
			return 0;		
		}
		
		wispy_phy_setcalibration(pi, 1);
		
		// Configure the default sweep block
		// TODO: can we change this?
		wispy_phy_setposition(pi, rangeset[x],0,0);
	}
	
	wispy_device_scan_free(&list);
		printf("x");
		fflush(stdout);
		
	return 1;
}

jintArray
Java_com_gnychis_coexisyst_CoexiSyst_pollWiSpy( JNIEnv* env, jobject thiz)
{
	int x,r;
	fd_set rfds;
	fd_set wfds;
	int maxfd = 0;
	struct timeval tm;
	wispy_sample_sweep *sb;
	jintArray result = 0;
	

	FD_ZERO(&rfds);
	FD_ZERO(&wfds);
	
	pi = devs;
	while(pi != NULL) {
		if(wispy_phy_getpollfd(pi) >= 0) {
			FD_SET(wispy_phy_getpollfd(pi), &rfds);
			
			if(wispy_phy_getpollfd(pi) > maxfd)
				maxfd = wispy_phy_getpollfd(pi);
		}
		pi = pi->next;
	}
	
	// Polling timeout, which also ratelimits the higher layer java function calling it
	tm.tv_sec = 0;
	tm.tv_usec = 10000;
	
	if(select(maxfd + 1, &rfds, &wfds, NULL, &tm) < 0) {
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "wispy_raw select() error: %s",
			strerror(errno));
		return result;
	}
	
	pi = devs;
	while(pi != NULL) {
		wispy_phy *di = pi;
		pi = pi->next;
		
		if(wispy_phy_getpollfd(di) < 0) {
			if(wispy_get_state(di) == WISPY_STATE_ERROR) {
				__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "error polling WiSpy device %s",
					wispy_phy_getname(di));
				return result;
			}
			continue;
		}
		
		if(FD_ISSET(wispy_phy_getpollfd(di), &rfds) == 0) {
			continue;
		}
		
		do {
			r = wispy_phy_poll(di);
			
			if((r & WISPY_POLL_CONFIGURED)) {
					
				wispy_sample_sweep *ran = wispy_phy_getcurprofile(di);
				
				if(ran==NULL) {
					continue;
				}
				
				__android_log_print(ANDROID_LOG_INFO, LOG_TAG,
					"    %d%s-%d%s @ %0.2f%s, %d samples",
	               ran->start_khz > 1000 ?
	               ran->start_khz / 1000 : ran->start_khz,
	               ran->start_khz > 1000 ? "MHz" : "KHz",
	               ran->end_khz > 1000 ? ran->end_khz / 1000 : ran->end_khz,
	               ran->end_khz > 1000 ? "MHz" : "KHz",
	               (ran->res_hz / 1000) > 1000 ?
	                ((float) ran->res_hz / 1000) / 1000 : ran->res_hz / 1000,
	               (ran->res_hz / 1000) > 1000 ? "MHz" : "KHz",
	               ran->num_samples);
				
	        	continue;
			} else if((r & WISPY_POLL_ERROR)) {
				__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Error polling device - %s",
					wispy_phy_getname(di));
				__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "... error: %s", wispy_get_error(di));
				return result;
			} else if((r & WISPY_POLL_SWEEPCOMPLETE)) {
				sb = wispy_phy_getsweep(di);
				if(sb==NULL)
					continue;

				// Create an array for the results
				result = (jintArray)(*env)->NewIntArray(env, sb->num_samples);
				jint *fill = (int *)malloc(sizeof(int) * sb->num_samples);
        
				for(r = 0; r < sb->num_samples; r++) {
					fill[r] = WISPY_RSSI_CONVERT(sb->amp_offset_mdbm, sb->amp_res_mdbm,sb->sample_data[r]);
					//fprintf(fh, "%d ", v);
				}
				//fprintf(fh, "\n");
				
				//fflush(fh);
				(*env)->SetIntArrayRegion(env, (jintArray)result, (jsize)0, (jsize)sb->num_samples, fill);
				free(fill);
				return result;
			}
			
		} while ((r & WISPY_POLL_ADDITIONAL));
	}
	
	if(result==NULL)
		result = (jintArray)(*env)->NewIntArray(env, 1);
  
	return result;
}

jobjectArray
Java_com_gnychis_coexisyst_CoexiSyst_getWiSpyList( JNIEnv* env, jobject thiz)
{
	jobjectArray names = 0;
	int ndev = 0;
	int x,r;
	jstring str1, str2, str3;
	
	ndev = wispy_device_scan(&list);
	if (ndev > 0) {
    	rangeset = (int *) malloc(sizeof(int) * ndev);
    	memset(rangeset, 0, sizeof(int) * ndev);
  	}
	
	if(ndev <= 0)
		return names;
	names = (*env)->NewObjectArray(env, (jsize)ndev, (*env)->FindClass(env, "java/lang/String"), 0);
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "found %d WiSpy devices", ndev);
	
    for (x = 0; x < ndev; x++) {
      char tname[512];
      memset(tname, '\0', sizeof(tname));
      
      snprintf(tname, sizeof(tname), "Device %d: %s id %u\n",
           x, list.list[x].name, list.list[x].device_id);

      for (r = 0; r < list.list[x].num_sweep_ranges; r++) {
        
        wispy_sample_sweep *ran =
          &(list.list[x].supported_ranges[r]);
          
        

        snprintf(tname+strlen(tname), sizeof(tname)-strlen(tname), "  Range %d: \"%s\" %d%s-%d%s @ %0.2f%s, %d samples\n", r,
             ran->name,
             ran->start_khz > 1000 ?
             ran->start_khz / 1000 : ran->start_khz,
             ran->start_khz > 1000 ? "MHz" : "KHz",
             ran->end_khz > 1000 ? ran->end_khz / 1000 : ran->end_khz,
             ran->end_khz > 1000 ? "MHz" : "KHz",
             (ran->res_hz / 1000) > 1000 ?
                ((float) ran->res_hz / 1000) / 1000 : ran->res_hz / 1000,
             (ran->res_hz / 1000) > 1000 ? "MHz" : "KHz",
             ran->num_samples);
      }
	  str1 = (*env)->NewStringUTF( env, tname );
	  (*env)->SetObjectArrayElement(env, names, x, str1);
	  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "WiSpy description: %s", tname);
    }
    
    wispy_device_scan_free(&list);

	return names;
}

jint
Java_com_gnychis_coexisyst_CoexiSyst_getWiSpy( JNIEnv* env, jobject thiz)
{
	devh = libusb_open_device_with_vid_pid(NULL, 0x1781, 0x083f);
	return devh ? 1 : -1;
}

jobjectArray
Java_com_gnychis_coexisyst_CoexiSyst_USBcheckForDevice( JNIEnv* env, jobject thiz, jint vid, jint pid )
{
	struct usb_bus *bus;
  	jobjectArray names = 0;
	jstring      str;
  	jsize        len = 0;
  		
	//__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "checking for a device!"); 	 
  
  	if(usb_find_busses()<0)
  		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "error finding USB busses"); 	 
	if(usb_find_devices()<0)
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "error finding USB devices"); 
		
	// Loop through and get all of the devices
	for (bus = usb_busses; bus; bus = bus->next) {
		if (bus->root_dev) { 	
			struct usb_device *dev;
			dev = bus->root_dev;
			if(dev->descriptor.idVendor==vid && dev->descriptor.idProduct==pid)
				return 1;
		} else {
      		struct usb_device *dev;

      		for (dev = bus->devices; dev; dev = dev->next) {
				if(dev->descriptor.idVendor==vid && dev->descriptor.idProduct==pid)
					return 1;
      		}
		}
	}
	
	return 0;
}

jobjectArray
Java_com_gnychis_coexisyst_CoexiSyst_getDeviceNames( JNIEnv* env, jobject thiz )
{
	struct usb_bus *bus;
  	jobjectArray names = 0;
	jstring      str;
  	jsize        len = 0;
	int          i=0;
	
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, 
            "in getDeviceNames() within driver"); 
  
  if(usb_find_busses()<0)
  	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "error finding USB busses"); 	 
	if(usb_find_devices()<0)
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "error finding USB devices"); 
		
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "USB bus head address: 0x%x", usb_busses);
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "USB bus head address: 0x%x", usb_busses->next);
	
	// Find the number of devices
	for (bus = usb_busses; bus; bus = bus->next) {
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "walking through busses");
		if (bus->root_dev) {
			len++;
		} else {
      		struct usb_device *dev;

      		for (dev = bus->devices; dev; dev = dev->next)
      			len++;
		}	
	}
 
 	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, 
            "the number of USB devices: %d", len); 
 
  	// Create an array for the devices
	names = (*env)->NewObjectArray(env, len, (*env)->FindClass(env, "java/lang/String"), 0);

	// Loop through and get all of the devices
	i=0;
	for (bus = usb_busses; bus; bus = bus->next) {
		if (bus->root_dev) { 	
			char name_buf[512];
			get_device_name(bus->root_dev, 0, name_buf);
			str = (*env)->NewStringUTF( env, name_buf );
			(*env)->SetObjectArrayElement(env, names, i, str);	
			__android_log_print(ANDROID_LOG_INFO, LOG_TAG, 
            	"has USB device: %s", name_buf); 
            i++;
		} else {
      		struct usb_device *dev;

      		for (dev = bus->devices; dev; dev = dev->next) {
				char name_buf[512];
				get_device_name(dev, 0, name_buf);
				str = (*env)->NewStringUTF( env, name_buf );
				(*env)->SetObjectArrayElement(env, names, i, str);	
				__android_log_print(ANDROID_LOG_INFO, LOG_TAG, 
	            	"has USB device: %s", name_buf);
	           	i++;
      		}
		}
	}
	
	return names;
}
