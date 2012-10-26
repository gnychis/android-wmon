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
#include <libusb.h>
#include <android/log.h>
#include "spectool_container.h" 
#include "spectool_net_client.h"
#include <errno.h>
#define LOG_TAG "AWMonDriver" // text for log tag 

// For keeping track of the devices, made global to handle callbacks and still
// have the device information
static struct libusb_device_handle *devh = NULL;
wispy_device_list wispy_list;
wispy_phy *wispy_pi;
wispy_phy *wispy_devs = NULL;
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
jint
Java_com_gnychis_awmon_Core_USBMon_initUSB( JNIEnv* env, jobject thiz )
{
  int r;
  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "entering initUSB");
  r = libusb_init(NULL);
  if(r < 0) {
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "failed to initialize libusb");
    return -1;
  } else {
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "successfully initialized libusb");
    return 1;
  }
}

int print_device(struct usb_device *dev, int level)
{
  usb_dev_handle *udev;
  char description[256];
  char string[256];
  int ret, i;

  udev = usb_open(dev);
  if (udev) {
    if (dev->descriptor.iManufacturer) {
      ret = usb_get_string_simple(udev, dev->descriptor.iManufacturer, string, sizeof(string));
      if (ret > 0)
        snprintf(description, sizeof(description), "%s - ", string);
      else
        snprintf(description, sizeof(description), "%04X - ",
                 dev->descriptor.idVendor);
    } else
      snprintf(description, sizeof(description), "%04X - ",
               dev->descriptor.idVendor);

    if (dev->descriptor.iProduct) {
      ret = usb_get_string_simple(udev, dev->descriptor.iProduct, string, sizeof(string));
      if (ret > 0)
        snprintf(description + strlen(description), sizeof(description) -
                 strlen(description), "%s", string);
      else
        snprintf(description + strlen(description), sizeof(description) -
                 strlen(description), "%04X", dev->descriptor.idProduct);
    } else
      snprintf(description + strlen(description), sizeof(description) -
               strlen(description), "%04X", dev->descriptor.idProduct);

  } else
    snprintf(description, sizeof(description), "%04X - %04X",
             dev->descriptor.idVendor, dev->descriptor.idProduct);

  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "%.*sDev #%d: %s\n", level * 2, "                    ", dev->devnum,
         description);

  if (udev) {
    if (dev->descriptor.iSerialNumber) {
      ret = usb_get_string_simple(udev, dev->descriptor.iSerialNumber, string, sizeof(string));
      if (ret > 0)
      __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "%.*s  - Serial Number: %s\n", level * 2,
               "                    ", string);
    }
  }

  if (udev)
    usb_close(udev);

  return 0;
}

jint
Java_com_gnychis_awmon_Core_USBMon_USBList( JNIEnv* env, jobject thiz )
{
  struct usb_bus *bus;

  usb_init();
  usb_find_busses();
  usb_find_devices();

  for (bus = usb_busses; bus; bus = bus->next) {
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "bus: 0x%x (%s - %u)", bus,bus->dirname,bus->location);
    if (bus->root_dev) {
      __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "root_dev: 0x%x", bus->root_dev);
      print_device(bus->root_dev, 0);
    } else {
      struct usb_device *dev;

      for (dev = bus->devices; dev; dev = dev->next) {
        __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "dev: 0x%x", dev);
        print_device(dev, 0);
      }
    }
  }
}

jobjectArray
Java_com_gnychis_awmon_Core_USBMon_GetUSBList( JNIEnv* env, jobject thiz )
{
  ssize_t cnt;
  libusb_device **devs;
	libusb_device *dev;
  int i=0;
  jobjectArray devList = 0;
	jstring      str;
  
  // First, just count the number of devices
  cnt = libusb_get_device_list(NULL, &devs);
  if(cnt <= 0)
    return NULL;
  
  // Create a list to fill in
  devList = (*env)->NewObjectArray(env, cnt, (*env)->FindClass(env, "java/lang/String"), 0);

  // Now go through each device
	while ((dev = devs[i++]) != NULL) {

		struct libusb_device_descriptor desc;
		int r = libusb_get_device_descriptor(dev, &desc);
		if (r < 0) { return NULL; }

    char str_buf[512];
    snprintf(str_buf, 512, "%d:%d", desc.idVendor, desc.idProduct);
    str = (*env)->NewStringUTF( env, str_buf );
    (*env)->SetObjectArrayElement(env, devList, i-1, str);
  }
  libusb_free_device_list(devs, 1);
	
  return devList;
}

jint
Java_com_gnychis_awmon_AWMon_Core_USBMon( JNIEnv* env, jobject thiz, jint vid, jint pid )
{
  ssize_t cnt;
  libusb_device **devs;
	libusb_device *dev;
  int i=0,ret;
  
  //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "entering USBcheckForDevice");

  // Get the usb device list
  cnt = libusb_get_device_list(NULL, &devs);
  //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "device count: %d\n", cnt);
  if(cnt < 0)
    return -1;

  // Go through the devices and see if the one we are looking for exists
  ret=0;
	while ((dev = devs[i++]) != NULL) {
		struct libusb_device_descriptor desc;
		int r = libusb_get_device_descriptor(dev, &desc);
		if (r < 0) {
			return -1;
		}

    if(desc.idVendor==vid && desc.idProduct==pid) {
      ret=1;
      break;
    }
	}

  libusb_free_device_list(devs, 1);
  
  /*if(ret==1)
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "..device found!");
  else
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "..device not found");
  */

  return ret;
}

jint
Java_com_gnychis_awmon_AWMon_USBcheckForDevice( JNIEnv* env, jobject thiz, jint vid, jint pid )
{
  ssize_t cnt;
  libusb_device **devs;
	libusb_device *dev;
  int i=0,ret;
  
  //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "entering USBcheckForDevice");

  // Get the usb device list
  cnt = libusb_get_device_list(NULL, &devs);
  //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "device count: %d\n", cnt);
  if(cnt < 0)
    return -1;

  // Go through the devices and see if the one we are looking for exists
  ret=0;
	while ((dev = devs[i++]) != NULL) {
		struct libusb_device_descriptor desc;
		int r = libusb_get_device_descriptor(dev, &desc);
		if (r < 0) {
			return -1;
		}

    if(desc.idVendor==vid && desc.idProduct==pid) {
      ret=1;
      break;
    }
	}

  libusb_free_device_list(devs, 1);
  
  /*if(ret==1)
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "..device found!");
  else
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "..device not found");
  */

  return ret;
}

void
Java_com_gnychis_awmon_AWMon_libusbTest( JNIEnv* env, jobject thiz )
{
	libusb_device **devs;
	int r;
	ssize_t cnt;

	r = libusb_init(NULL);
	if (r < 0)
		return r;

	cnt = libusb_get_device_list(NULL, &devs);
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "libusb count: %d\n", cnt);

	libusb_free_device_list(devs, 1);

	libusb_exit(NULL);
}

jobjectArray
Java_com_gnychis_awmon_AWMon_getDeviceNames( JNIEnv* env, jobject thiz )
{
	struct usb_bus *bus;
  struct usb_bus *busses;
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

	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "usb_find_busses(): %d\n", usb_find_busses());
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "usb_find_devices(): %d\n", usb_find_devices());
		
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "USB bus head address: 0x%x", usb_busses);
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "USB bus head address: 0x%x", usb_busses->next);

//  busses = usb_get_busses();
  for (bus = usb_busses; bus; bus = bus->next) {
    if (bus->root_dev) {
      __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "...RootDev");
      len++;
    } else {
      struct usb_device *dev;

      for (dev = bus->devices; dev; dev = dev->next) {
        __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "...Dev");
        len++;
      }
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

jint
Java_com_gnychis_awmon_DeviceHandlers_WiSpy_initWiSpyDevices( JNIEnv* env, jobject thiz )
{
	int x;
	sample = 0;
	
	ndev = wispy_device_scan(&wispy_list);

	fh = fopen("/sdcard/awmon_raw.txt","w+");
	
	// Make sure that a device is connected
	if(ndev <= 0) {
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "There seem to be no devices listed as WiSpy devices...\n");
		return 0;
	}

	if (ndev > 0) {
    	rangeset = (int *) malloc(sizeof(int) * ndev);
    	memset(rangeset, 0, sizeof(int) * ndev);
  	}
		
	// Initialize each of the devices
	for(x = 0; x < ndev; x++) {
			
		wispy_pi = (wispy_phy *) malloc(WISPY_PHY_SIZE);
		wispy_pi->next = wispy_devs;
		wispy_devs = wispy_pi;
		
		if(wispy_device_init(wispy_pi, &(wispy_list.list[x])) < 0) {
		  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Unable to initialize the WiSpy device\n");
			return 0;
		}
		
		if(wispy_phy_open(wispy_pi) < 0) {
      __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Unable to open the WiSpy physical layer\n");
			return 0;		
		}
		
		wispy_phy_setcalibration(wispy_pi, 1);
		
		// Configure the default sweep block
		// TODO: can we change this?
		wispy_phy_setposition(wispy_pi, rangeset[x],0,0);
	}
	
	wispy_device_scan_free(&wispy_list);
  printf("x");
  fflush(stdout);
		
	return 1;
}

jchar
Java_com_gnychis_awmon_Core_USBSerial_blockRead1(JNIEnv* env, jobject thiz, int fd) {
	int n;
	char c;

	while(1) {
		if((n = read(fd, &c, 1))==1) {
			return c;
		} else if(n==-1) {
			 jclass newExcCls;
			 (*env)->ExceptionDescribe(env);
			 (*env)->ExceptionClear(env);
			 newExcCls = (*env)->FindClass(env, 
										 "java/lang/IllegalArgumentException");
			 if (newExcCls == NULL) {
					 /* Unable to find the exception class, give up. */
					 return;
			 }
			 (*env)->ThrowNew(env, newExcCls, "error reading from serial device with blockRead1");
		}
	}
}

jchar blockRead1(JNIEnv* env, jobject thiz, int fd) {
	int n;
	char c;

	while(1) {
		if((n = read(fd, &c, 1))==1) {
			return c;
		} else if(n==-1) {
			 jclass newExcCls;
			 (*env)->ExceptionDescribe(env);
			 (*env)->ExceptionClear(env);
			 newExcCls = (*env)->FindClass(env, 
										 "java/lang/IllegalArgumentException");
			 if (newExcCls == NULL) {
					 /* Unable to find the exception class, give up. */
					 return;
			 }
			 (*env)->ThrowNew(env, newExcCls, "error reading from serial device with blockRead1");
		}
	}
}

jint
Java_com_gnychis_awmon_Core_USBSerial_readInt32(JNIEnv* env, jobject thiz, int fd) {
	int i;
	uint32_t v = 0;

	for(i=0;i<4;i++) {
		uint32_t t;
		
		t= ((uint32_t)blockRead1(env, thiz, fd)) & 0xff;
		v = v | (t << i*CHAR_BIT);
	}

	return v;
}

void
Java_com_gnychis_awmon_Core_USBSerial_writeBytes(JNIEnv* env, jobject thiz, int fd, jbyteArray data, int length)
{
	int nwrote=0;
	int n;
	char *pBuf;
	pBuf = (char *) (*env)->GetByteArrayElements(env, data, NULL);
	while(nwrote<length) {
		n=write(fd, pBuf+nwrote, length-nwrote);

		if(n==-1) {
			 jclass newExcCls;
			 (*env)->ExceptionDescribe(env);
			 (*env)->ExceptionClear(env);
			 newExcCls = (*env)->FindClass(env, 
										 "java/lang/IllegalArgumentException");
			 if (newExcCls == NULL) {
					 /* Unable to find the exception class, give up. */
					 return;
			 }
			 (*env)->ThrowNew(env, newExcCls, "error writing to serial device");
		}

		nwrote+=n;
	}
	(*env)->ReleaseByteArrayElements(env, data, pBuf, 0);
}


jbyteArray
Java_com_gnychis_awmon_Core_USBSerial_blockReadBytes(JNIEnv* env, jobject thiz, int fd, int nbytes) {
	int nread=0;
	jbyteArray buf;
	char *pBuf;

	buf = (*env)->NewByteArray(env, nbytes);

	pBuf = (char *) (*env)->GetByteArrayElements(env, buf, NULL);

	while(nread<nbytes) {
		int n = read(fd, pBuf+nread, nbytes-nread);
		if(n==-1) {
			 jclass newExcCls;
			 (*env)->ExceptionDescribe(env);
			 (*env)->ExceptionClear(env);
			 newExcCls = (*env)->FindClass(env, 
										 "java/lang/IllegalArgumentException");
			 if (newExcCls == NULL) {
					 /* Unable to find the exception class, give up. */
					 return;
			 }
			 (*env)->ThrowNew(env, newExcCls, "error reading from serial device with blockReadBytes");
		}
		nread += n;
	}
	(*env)->ReleaseByteArrayElements(env, buf, pBuf, 0);
	return buf;
}

jint
Java_com_gnychis_awmon_Core_USBSerial_closeCommPort(JNIEnv* env, jobject thiz, jint fd)
{
	return close(fd);
}

jint
Java_com_gnychis_awmon_Core_USBSerial_openCommPort(JNIEnv* env, jobject thiz, jstring port_name)
{
	const char *nativePort = (*env)->GetStringUTFChars(env, port_name, 0);
	int fd = open (nativePort, O_RDWR | O_NOCTTY | O_SYNC);

	if (fd < 0)
	{
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "ERROR OPENING COMM PORT!\n");
		return -1;
	}

	set_interface_attribs (fd, B115200, 0);  // set speed to 115,200 bps, 8n1 (no parity)
	set_blocking (fd, 0);                // set no blocking
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Success in opening comm port\n");
	
	(*env)->ReleaseStringUTFChars(env, port_name, nativePort);

	return fd;
}

jintArray
Java_com_gnychis_awmon_DeviceHandlers_WiSpy_pollWiSpy( JNIEnv* env, jobject thiz)
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
	
	wispy_pi = wispy_devs;
	while(wispy_pi != NULL) {
		if(wispy_phy_getpollfd(wispy_pi) >= 0) {
			FD_SET(wispy_phy_getpollfd(wispy_pi), &rfds);
			
			if(wispy_phy_getpollfd(wispy_pi) > maxfd)
				maxfd = wispy_phy_getpollfd(wispy_pi);
		}
		wispy_pi = wispy_pi->next;
	}
	
	// Polling timeout, which also ratelimits the higher layer java function calling it
	tm.tv_sec = 0;
	tm.tv_usec = 10000;
	
	if(select(maxfd + 1, &rfds, &wfds, NULL, &tm) < 0) {
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "wispy_raw select() error: %s",
			strerror(errno));
		return result;
	}
	
	wispy_pi = wispy_devs;
	while(wispy_pi != NULL) {
		wispy_phy *di = wispy_pi;
		wispy_pi = wispy_pi->next;
		
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
				__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "got a sweep, sb: 0x%x", sb);
				if(sb==NULL)
					continue;

				// Create an array for the results
				result = (jintArray)(*env)->NewIntArray(env, sb->num_samples);
				jint *fill = (int *)malloc(sizeof(int) * sb->num_samples);
        
				__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "WiSpy polled for %d samples", sb->num_samples);
				for(r = 0; r < sb->num_samples; r++) {
					fill[r] = WISPY_RSSI_CONVERT(sb->amp_offset_mdbm, sb->amp_res_mdbm,sb->sample_data[r]);
					fprintf(fh, "%d ", fill[r]);
				}
				fprintf(fh, "\n");
				
				fflush(fh);
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
Java_com_gnychis_awmon_AWMon_getWiSpyList( JNIEnv* env, jobject thiz)
{
	jobjectArray names = 0;
	int ndev = 0;
	int x,r;
	jstring str1, str2, str3;
	
	ndev = wispy_device_scan(&wispy_list);
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
           x, wispy_list.list[x].name, wispy_list.list[x].device_id);

      for (r = 0; r < wispy_list.list[x].num_sweep_ranges; r++) {
        
        wispy_sample_sweep *ran =
          &(wispy_list.list[x].supported_ranges[r]);
          
        

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
    
    wispy_device_scan_free(&wispy_list);

	return names;
}

jobjectArray
Java_com_gnychis_awmon_DeviceHandlers_WiSpy_getWiSpyList( JNIEnv* env, jobject thiz)
{
	jobjectArray names = 0;
	int ndev = 0;
	int x,r;
	jstring str1, str2, str3;
	
	ndev = wispy_device_scan(&wispy_list);
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
           x, wispy_list.list[x].name, wispy_list.list[x].device_id);

      for (r = 0; r < wispy_list.list[x].num_sweep_ranges; r++) {
        
        wispy_sample_sweep *ran =
          &(wispy_list.list[x].supported_ranges[r]);
          
        

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
    
    wispy_device_scan_free(&wispy_list);

	return names;
}

jint
Java_com_gnychis_awmon_DeviceHandlers_WiSpy_getWiSpy( JNIEnv* env, jobject thiz)
{
	devh = libusb_open_device_with_vid_pid(NULL, 0x1781, 0x083f);
	return devh ? 1 : -1;
}

