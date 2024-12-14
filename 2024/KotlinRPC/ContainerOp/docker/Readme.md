# Build
```shell
sudo docker build -t kyoyap/devenv:ctrop .
```

# Push / Pull
```shell
sudo docker login
sudo docker push kyoyap/devenv:ctrop
sudo docker pull kyoyap/devenv:ctrop
sudo docker logout
```

```shell
# ctrで実行
ctr i pull -u kyoyap:******** docker.io/kyoyap/devenv:ctrop
OPTS='--mount type=bind,src=/var/run/containerd/containerd.sock,dst=/var/run/containerd/containerd.sock,options=rbind:rw'

ctr run --rm --no-pivot --net-host $OPTS docker.io/kyoyap/devenv:ctrop c1
ctr run --rm --no-pivot --net-host $OPTS docker.io/kyoyap/devenv:ctrop c1 java -jar server-all.jar -P:ktor.deployment.port=8081
```
