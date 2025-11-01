# Steg 1: Byggfas (Används endast för att skapa de statiska filerna)
FROM node:20-alpine AS build
WORKDIR /app

# Kopiera beroende-filer (package.json och package-lock.json) först för att utnyttja cache
COPY package.json .
COPY package-lock.json .

# Installera dependencies
# npm ci är snabbare och säkrare än npm install
RUN npm ci

# Kopiera källkoden och kör bygg-steget
COPY . .
RUN npm run build

# ------------------------------------------------------------------

# Steg 2: Körningsfas (Används endast för att servera de statiska filerna)
# Använd en lättvikts webbserver (NGINX) för att servera de statiska filerna
FROM nginx:alpine
LABEL author="PatientSystem Frontend"

# EXPOSE är endast dokumentation, men bra praxis
EXPOSE 80 

# Kopiera de statiska filerna från byggfasen till NGINX standard-serverkatalog
# 'build' mappen är standard output från 'npm run build'
COPY --from=build /app/build /usr/share/nginx/html

# NGINX startar automatiskt som ENTRYPOINT, så vi behöver inget CMD/ENTRYPOINT
# Men vi ändrar porten till 3000 i NGINX konfig
COPY nginx.conf /etc/nginx/conf.d/default.conf