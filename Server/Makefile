TARGET = SoundListener
CC = gcc

$(TARGET) : SoundListener.o
	$(CC) -o $@ SoundListener.o 

server.o : SoundListener.c
	$(CC) -c $? 

clean:
	rm -f SoundListener.o SoundListener
