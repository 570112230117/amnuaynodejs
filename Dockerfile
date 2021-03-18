FROM node:10 
ENV NODE_ENV=development NODE_PATH=/usr/src/app
WORKDIR /usr/src/app
COPY package*.json ./
RUN npm install
# RUN npm audit fix
# Bundle app source
COPY . .
CMD [ "npm", "start" ]

