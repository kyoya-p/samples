FROM node:18-slim

WORKDIR /home/node/app

COPY package*.json ./
RUN npm install

COPY . .

EXPOSE 3000

CMD ["node", "index.js"]
