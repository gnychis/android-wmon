#!/bin/bash
curl http://www.dns-sd.org/ServiceTypes.html > /tmp/service_types.html
grep "<b>" /tmp/service_types.html | awk -F'<b>' '{print $2}' | awk -F'</b>' '{print $1}' | grep -v NOTE | grep -v NOT | grep -v "fifteen char" > mdns_sevice_types.txt
