# 概要

CloudRunでwebsocketのテスト。
定期的に応答を返すwebsocketサーバを作成。

# プロジェクト設定
```sh:Typescript Project生成
npm init -y
npm install typescript @types/node --save-dev
npx tsc --init --rootDir src --outDir build
```

# ビルド・実行・確認
```sh
npm i
npx ts-node src/index.ts
curl http://localhost:8080
```

# Docker ビルド・実行・確認・削除
```sh
sudo docker build -t wsdemo DockerBuild --no-cache
sudo docker run --name wsdemo -p 8080:8080 wsdemo
curl http://localhost:8080
sudo docker rm -f wsdemo
```

# Google リポジトリ準備
[参考](https://cloud.google.com/artifact-registry/docs/repositories/create-repos?authuser=1&hl=ja)
[参考](https://cloud.google.com/artifact-registry/docs/docker/store-docker-container-images?hl=ja)
[参考](https://cloud.google.com/artifact-registry/docs/access-control?hl=ja#gcloud)

```sh:ログイン・初期化・パブリッシュ権限付与
gcloud auth login
gcloud init

PROJ=$(gcloud config get-value project)
USER=shokkaa@gmail.com
#gcloud projects add-iam-policy-binding $PROJ --role=roles/artifactregistry.repoAdmin --member=user:$USER
```
```sh:リポジトリ作成・Dockerリポジトリ認証設定
REPOS=r1
REGION=us-central1
#gcloud artifacts repositories create $REPOS --location=asia-northeast1 --repository-format=docker
#gcloud artifacts repositories create $REPOS --repository-format docker --location asia-northeast1
gcloud artifacts repositories create $REPOS --repository-format docker --location $REGION
gcloud artifacts repositories list

gcloud auth configure-docker $REGION-docker.pkg.dev
export PROJECT_ID="${PROJ}"
gcloud builds submit --region $REGION --config cloudbuild.yaml
```
```sh:イメージビルド・パブリッシュ
TAG=asia-northeast1-docker.pkg.dev/$PROJ/$REPOS/hello
sudo docker build DockerBuild -t $TAG
sudo docker push $TAG
```

