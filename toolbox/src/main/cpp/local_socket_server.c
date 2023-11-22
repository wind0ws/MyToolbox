#include <malloc.h>
#include <unistd.h>
#include <pthread.h>
#include <sys/socket.h>
#include <netinet/in.h>
//#include <arpa/inet.h>
#include <sys/un.h> /* for struct sockaddr_un */
#include <stdbool.h>
#include <string.h>
#include <stddef.h>
#include "local_socket_server.h"
#define LOG_TAG "JNI_L_SRV"
#include "lcu/log/logger.h"

#define LOCAL_SOCKET_SERVER_THREAD_NAME_MAX_LENGTH 128

typedef struct socket_server_s {
    server_config_t config;
    pthread_mutex_t thread_mutex;
    pthread_t process_thread_id;
    int client_fd;
    bool flag_exit;
} socket_server_t;


static size_t read_buffer(const int sock_fd, char *buffer, const size_t reqLen) {
    char *cur_buffer = buffer;
    size_t bytes_left = reqLen;
    ssize_t bytes_read;
    while (bytes_left > 0 &&
           (bytes_read = read(sock_fd, cur_buffer, bytes_left)) > 0) {
        cur_buffer += bytes_read;
        bytes_left -= bytes_read;
    }
    return (cur_buffer - buffer);
}

static void close_socket(socket_server_t *socket_server) {
    pthread_mutex_lock(&socket_server->thread_mutex);
    if (socket_server->client_fd != -1) {
        shutdown(socket_server->client_fd, SHUT_RDWR);
        close(socket_server->client_fd);
        socket_server->client_fd = -1;
    }
    pthread_mutex_unlock(&socket_server->thread_mutex);
}

static void *thread_fun_socket_server(void *thread_context) {
    socket_server_t *socket_server = thread_context;
    int ret_code = 0;

    while (!(socket_server->flag_exit)) {
        int server_sockfd = -1;
        /* 声明一个UNIX域套接字结构 */
        struct sockaddr_un server_address;
        struct sockaddr_un client_address;

        /* 删除原有server_socket对象 */
        unlink(socket_server->config.socket_name);

        /*创建 socket, 通信协议为AF_UNIX, SOCK_STREAM 数据方式*/
        server_sockfd = socket(AF_UNIX, SOCK_STREAM, 0);
        if (server_sockfd < 0) {
            LOGE("failed to create socket. %d", server_sockfd);
            sleep(2);
            goto label_close_server_sockfd;
        }
        /* 配置服务器信息(通信协议) */
        server_address.sun_family = AF_UNIX;

        /* 配置服务器信息(socket 对象) */
        server_address.sun_path[0] = 0;
        strcpy(server_address.sun_path + 1, socket_server->config.socket_name);

        /* 绑定 socket 对象 */
        ret_code = bind(server_sockfd, (struct sockaddr *) &server_address,
                           offsetof(struct sockaddr_un, sun_path) + 1 +
                           strlen(socket_server->config.socket_name));
        if (ret_code != 0) {
            LOGE("failed to bind server socket retCode=%d ..", ret_code);
            sleep(2);
            goto label_close_server_sockfd;
        }
        LOGI("successful bind server socket...");
        /* 监听网络,队列数为1 */
        ret_code = listen(server_sockfd, 1);
        if (ret_code != 0) {
            LOGE("failed(%d) to listen on server socket.", ret_code);
            sleep(1);
            goto label_close_server_sockfd;
        }

        /* 关闭socket服务端 */
        label_close_server_sockfd:
        if (server_sockfd != -1) {
            close(server_sockfd);
            server_sockfd = -1;
        }
    }
    return NULL;
}

int local_socket_server_start(__OUT server_handle *handle_p, __IN server_config_t *config_p) {
    int ret = -1;
    socket_server_t *socket_server = (socket_server_t *)malloc(sizeof(socket_server_t));
    if (!socket_server) {
        LOGE("failed on alloc socket_server_t memory");
        return ret;
    }
    socket_server->process_thread_id = -1;
    socket_server->config = *config_p;
    socket_server->flag_exit = false;
    pthread_mutex_init(&socket_server->thread_mutex, NULL);
    ret = pthread_create(&(socket_server->process_thread_id), NULL,
                             thread_fun_socket_server, socket_server);
    if (ret == 0) {
        *handle_p = socket_server;
        char temp[LOCAL_SOCKET_SERVER_THREAD_NAME_MAX_LENGTH];
        snprintf(temp, LOCAL_SOCKET_SERVER_THREAD_NAME_MAX_LENGTH,
                 "l_srv_%s", socket_server->config.socket_name);
        pthread_setname_np(socket_server->process_thread_id, temp);
    } else {
        *handle_p = NULL;
        free(socket_server);
    }
    return ret;
}

int local_socket_server_stop(__IN server_handle *handle_p) {
    socket_server_t *socket_server = *handle_p;
    if (socket_server == NULL) {
        return -1;
    }
    socket_server->flag_exit = true;
    if (socket_server->process_thread_id != -1) {
        close_socket(socket_server);
        pthread_join(socket_server->process_thread_id, NULL);
        socket_server->process_thread_id = -1;
    }
    pthread_mutex_destroy(&socket_server->thread_mutex);
    free(socket_server);
    *handle_p = NULL;
    return 0;
}

