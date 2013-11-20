#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>

int getConnect(int port);
int getFileSize(int sock);
int getSoundFile(int sock, int fileSize, FILE* fp);

int main(void){
	// socket
	int sock;
	// file size
	int fileSize = 0;
	// file pointer
	FILE* fp;

	//counter
	int count = 0;

	// open the file
	fp = fopen("rec.raw", "wb" );
	if( fp == NULL ){
		fputs( "ERROR: File\n", stderr );
		exit( EXIT_FAILURE );
	}

	// connect client	
	sock = getConnect(12345);
	printf("connected\n");

	while(count < 100){
		// get file size
		fileSize = getFileSize(sock);
		if(fileSize == -1){
			printf("ERROR\n");
			break;
		}
		else{
			printf("file size: %d\n", fileSize);
		}

		// get sound data
		if(getSoundFile(sock, fileSize, fp) == -1){
			break;
		}
		count++;
	}

	
	//file close
	fclose(fp);
	// close socket
	close(sock);
	return 0;
}

int getConnect(int port){
	struct sockaddr_in addr;
	struct sockaddr_in client;
	int len;
	int sock, sock0;
	int yes = 1;

	sock0 = socket(AF_INET, SOCK_STREAM, 0);

	addr.sin_family = AF_INET;
	addr.sin_port = htons(port);
	addr.sin_addr.s_addr = INADDR_ANY;
	//make it available immediately to connect
	setsockopt(sock0,SOL_SOCKET, SO_REUSEADDR, (const char *)&yes, sizeof(yes));
	bind(sock0, (struct sockaddr *)&addr, sizeof(addr));
	listen(sock0, 5);
	len = sizeof(client);

	sock = accept(sock0, (struct sockaddr *)&client, &len);
	if(sock == -1){
		printf("ERROR: socket cannot accept\n");
		return -1;
	}
	
	return sock;
}

int getFileSize(int sock){
	int fileSize = 0;

	// recive data of file size	
	if(recv(sock, &fileSize, sizeof(fileSize), 0) == -1){
		printf("ERROR: can not recieve info\n");
		return -1;
	}

	return fileSize;
}

int getSoundFile(int sock, int fileSize, FILE* fp){
	int recieveSize = 0, recieveSum = 0;
	char soundData[fileSize];


	while(1){
		// recive data of file size
		recieveSize = recv(sock, soundData + recieveSum, fileSize - recieveSum, 0);
		printf("recieve size: %d\n", recieveSize);
		if(recieveSize == -1 || recieveSize == 0){
			printf("ERROR: can not recieve data\n");
			return -1;
		}
		recieveSum += recieveSize;
		if(recieveSum == fileSize)
			break;
		else if(recieveSum > fileSize){
			printf("ERROR: recive data over\n");
                        return -1;
		}
	}

	//write sound data
	fwrite(soundData, sizeof( char ), fileSize, fp);
}
