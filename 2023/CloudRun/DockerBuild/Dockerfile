FROM node:alpine3.18

WORKDIR /app
RUN apk add git
RUN git clone --depth 1  https://github.com/kyoya-p/samples
WORKDIR /app/samples/2023/CloudRun
RUN npm i
RUN npx tsc
RUN node build/index.js
