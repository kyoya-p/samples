FROM node:alpine3.18

WORKDIR /app
RUN apk add git
RUN git clone --depth 1  https://github.com/kyoya-p/samples
RUN cd samples/2023/CloudRun
RUN npx tsc
RUN node build/inde.js
