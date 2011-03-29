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
#include <usb.h>
#include "spectool_container.h" 
#include "spectool_net_client.h"
#include <errno.h>

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

int main()
{
	if(initWiSpyDevices()==0)
		exit(-1);

	while(1) {
		if(pollWiSpy() == -1)
			exit(-1);

		printf(".");
		fflush(stdout);
	}
}


int initWiSpyDevices()
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

int pollWiSpy()
{
	int x,r;
	fd_set rfds;
	fd_set wfds;
	int maxfd = 0;
	struct timeval tm;
	wispy_sample_sweep *sb;

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
		return -1;
	}
	
	pi = devs;
	while(pi != NULL) {
		wispy_phy *di = pi;
		pi = pi->next;
		
		if(wispy_phy_getpollfd(di) < 0) {
			if(wispy_get_state(di) == WISPY_STATE_ERROR) {
				return -1;
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
				
	             continue;
			} else if((r & WISPY_POLL_ERROR)) {
				return -1;
			} else if((r & WISPY_POLL_SWEEPCOMPLETE)) {
				sb = wispy_phy_getsweep(di);
				if(sb==NULL)
					continue;

        // Create an array for the results
        int *fill = (int *)malloc(sizeof(int) * sb->num_samples);

				for(r = 0; r < sb->num_samples; r++) {
					int v = WISPY_RSSI_CONVERT(sb->amp_offset_mdbm, sb->amp_res_mdbm,sb->sample_data[r]);
					fill[r] = v;
					fprintf(fh, "%d ", v);
				}
				fprintf(fh, "\n");
				
				fflush(fh);
				free(fill);
				return 1;
			}
			
		} while ((r & WISPY_POLL_ADDITIONAL));
	}

	return 1;
}

