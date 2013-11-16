#include <jni.h>
#include <android/log.h>

#include <stdio.h>
#include <stdlib.h>

#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>

#define LOG_TAG	"ghostear"
#define LOGI(...)	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)	__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

//Global variable
int sock;

// Convert Endian
int convertEndian(void *input, size_t s);

JNIEXPORT void JNICALL Java_com_ghostear_SoundManagementNative_connectServer(JNIEnv * env, jobject obj, jstring address, jint port_number ){
	struct sockaddr_in server;
	const char *address_number = (*env)->GetStringUTFChars(env, address, 0);

	/* create socket */
	sock = socket(AF_INET, SOCK_STREAM, 0);

	/* Preparation of the structure for the specified destination */
	server.sin_family = AF_INET;
	// default: 12335
	server.sin_port = htons(port_number);
	server.sin_addr.s_addr = inet_addr(address_number);

	/* connect to server */
	connect(sock, (struct sockaddr *)&server, sizeof(server));
}

JNIEXPORT void JNICALL Java_com_ghostear_SoundManagementNative_closeConnect(JNIEnv * env){
	if(close(sock)<0){
		printf("error_socket\n");
	}
}


JNIEXPORT void JNICALL Java_com_ghostear_SoundManagementNative_sendSoundData(JNIEnv * env,jobject thiz, jbyteArray data, jint fileSize){
	jbyte* data_array = (jbyte*)(*env)->GetPrimitiveArrayCritical(env, data, NULL);

	__android_log_print(ANDROID_LOG_DEBUG,"Native","size: %d",data_array[fileSize-1]);

	// send file size
	send(sock, &fileSize, sizeof(fileSize), 0);

	// little endian to big endian
	convertEndian(data_array, fileSize);

	// send sound data
	send(sock, data_array, fileSize, 0);

	(*env)->ReleasePrimitiveArrayCritical(env, data, data_array, 0);

}

int convertEndian(void *input, size_t s){
	int i;   // counter
	char *temp;   // temp

	if((temp = (char *)calloc( s, sizeof(char))) == NULL){
		return 0;   // error
	}

	for(i=0; i<s; i++){   // save input to temp
		temp[i] = ((char *)input)[i];
	}

	for(i=1; i<=s; i++){   // reverse
		((char *)input)[i-1] = temp[s-i];
	}

	free(temp);   // free

	return 1;   // finish
}

