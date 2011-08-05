all: zigcapd

zigcapd: zigcapd.o serial.o
	gcc -o zigcapd zigcapd.o serial.o

zigcapd.o: zigcapd.c
	gcc -Wall -c -o zigcapd.o zigcapd.c

serial.o: serial.c
	gcc -Wall -c -o serial.o serial.c
