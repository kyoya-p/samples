#include <stdio.h>
#include <stdlib.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <signal.h>

int main() {
  int fd = open("abc.txt",O_CREAT|O_RDWR|O_TRUNC,0644);
  if(fd < 0){
    perror("open");
    exit(1);
  }

  //  int fd = shm_open("shared_memory", O_CREAT | O_RDWR, 0666);
  //  if (fd == -1) {
  //    perror("shm_open");
  //    exit(1);
  //  }

  // 共有メモリをマップする
  void *ptr = mmap(NULL, 1024, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
  if (ptr == MAP_FAILED) {
    perror("mmap");
    exit(1);
  }

  // 共有メモリにデータを書き込む
  memcpy(ptr, "Hello, world!\n", 13);

  // 共有メモリに書き込み完了のシグナルを送信する
  kill(getppid(), SIGUSR1);

  // 共有メモリをアンマップする
  if (munmap(ptr, 1024) == -1) {
    perror("munmap");
    exit(1);
  }

  // 共有メモリファイルを閉じる
  close(fd);

  return 0;
}
