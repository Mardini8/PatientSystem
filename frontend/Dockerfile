# Build stage
FROM node:18-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

ARG REACT_APP_USER_SERVICE_URL=https://patientsystem-user.app.cloud.cbh.kth.se
ARG REACT_APP_MESSAGE_SERVICE_URL=https://patientsystem-message.app.cloud.cbh.kth.se
ARG REACT_APP_CLINICAL_SERVICE_URL=https://patientsystem-clinical.app.cloud.cbh.kth.se
ARG REACT_APP_IMAGE_SERVICE_URL=https://patientsystem-image.app.cloud.cbh.kth.se
ARG REACT_APP_SEARCH_SERVICE_URL=https://patientsystem-search.app.cloud.cbh.kth.se

RUN npm run build

# Production stage
FROM nginx:alpine

COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /app/build /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]