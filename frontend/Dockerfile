FROM node:20-alpine AS build
WORKDIR /app

COPY package.json .
COPY package-lock.json .

RUN npm ci

COPY . .
RUN npm run build

# ------------------------------------------------------------------
FROM nginx:alpine
LABEL author="PatientSystem Frontend"

EXPOSE 80

COPY --from=build /app/build /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf