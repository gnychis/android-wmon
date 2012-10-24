#!/bin/bash
for file in /data/data/$1/files/*; do 
  bn=$(basename $file)
  rm /system/bin/$bn
  ln -s $file /system/bin/$bn
done
