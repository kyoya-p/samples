#include <stdlib.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>

int main(void)
{
  printf("Copy Test\n");
  int fdi, fdo;
  int pagesize=sysconf(_SC_PAGESIZE);
  printf("pagesize:%d\n" ,pagesize);
  char *i, *o;
  fdi = open("in", O_RDONLY);
  fdo = open("out", O_RDWR);
  i = mmap(NULL, pagesize, PROT_READ, MAP_SHARED, fdi, 0);
  o = mmap(NULL, pagesize, PROT_WRITE, MAP_SHARED, fdo, 0);
  while (pagesize--){
    printf("%d\n",pagesize);
    //   *o++ = *i++;
    *o++ = 0x30;
  }
}
