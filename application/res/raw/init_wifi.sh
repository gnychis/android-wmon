#!/bin/bash
app_name="$1"
iface_name=""
while [ "$iface_name" == "" ]; do 
  iface_name=$(find /sys/devices/platform -name "*wlan*" | grep usb | awk -F'/' '{print $NF}')
  sleep 1
done
echo $iface_name

iface_up=$(netcfg | grep $iface_name | grep UP | wc -l)
moni0_exist=$(netcfg | grep moni0 | wc -l)
moni0_up=$(netcfg | grep moni0 | grep UP | wc -l)

# If both interfaces exist and are up... we can just exit
if [ $iface_up -eq 1 -a  $moni0_up -eq 1 ]; then exit; fi

# Otherwise, take the interface down to create the monitoring interface
netcfg $iface_name down

# If the monitoring interface does not exist, let's create it
if [ $moni0_exist -eq 0 ]; then
  phy_iface=$(/data/data/$app_name/files/iw list | head -n 1 | awk '{print $2}')
  /data/data/$app_name/files/iw phy $phy_iface interface add moni0 type monitor
fi
