#!/bin/sh
LEASE_DIR=${TMPDIR}
if [ -z "$LEASE_DIR" ]; then
	LEASE_DIR="/var/tmp"
fi
mkdir -p ${LEASE_DIR}
echo > ${LEASE_DIR}/izcoordinator.leases
LEASE_FILE=${LEASE_DIR}/izcoordinator.leases
exec ./parsetest ${LEASE_FILE}

