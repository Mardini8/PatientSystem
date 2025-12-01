# 📁 Översikt över alla filer i Kubernetes-mappen

## 📖 Dokumentation (5 filer)

| Fil | Storlek | Beskrivning | När du använder den |
|-----|---------|-------------|---------------------|
| **START_HERE.md** | 5.4 KB | Sammanfattning och välkomstsida | **BÖRJA HÄR** - läs denna först! |
| **QUICKSTART.md** | 6.3 KB | Steg-för-steg installationsguide | När du ska sätta upp första gången |
| **README.md** | 7.2 KB | Komplett dokumentation | För djupare förståelse och referens |
| **CHEATSHEET.md** | 4.3 KB | Snabbkommandon och tips | Daglig användning, spara som bookmark |
| **STRUCTURE.md** | 6.8 KB | Visuell översikt och arkitektur | När du vill förstå helheten |

**Total dokumentation: ~30 KB**

## 🚀 Deployment

| Fil | Beskrivning |
|-----|-------------|
| **deploy-all.sh** | Automatiskt deployment-script - kör detta för att deploya allt! |
| **namespace.yaml** | Skapar Kubernetes namespace "patientsystem" |

## 🗄️ MySQL Databas (3 filer)

```
mysql/
├── mysql-pvc.yaml            # Persistent storage (5GB)
├── mysql-deployment.yaml     # MySQL pod konfiguration
└── mysql-service.yaml        # Intern service för databas-access
```

**OBS! Uppdatera lösenord och databas-namn i mysql-deployment.yaml**

## 🔧 Microservices Konfiguration (6 × 2 = 12 filer)

Varje microservice har 2 filer: deployment + service

### 1. Clinical Service (Spring Boot)
```
clinical-service/
├── deployment.yaml    # Pod config (port 8080)
└── service.yaml       # Intern service
```

### 2. User Service (Spring Boot + MySQL)
```
user-service/
├── deployment.yaml    # Pod config (port 8081) + MySQL connection
└── service.yaml       # Intern service
```
⚠️ **Uppdatera MySQL credentials här!**

### 3. Message Service (Spring Boot + MySQL)
```
message-service/
├── deployment.yaml    # Pod config (port 8082) + MySQL connection
└── service.yaml       # Intern service
```
⚠️ **Uppdatera MySQL credentials här!**

### 4. Image Service (Node.js + MySQL)
```
image-service/
├── deployment.yaml    # Pod config (port 3001) + MySQL connection
└── service.yaml       # Intern service
```
⚠️ **Uppdatera MySQL credentials här!**

### 5. Search Service (Quarkus Reactive)
```
search-service/
├── deployment.yaml    # Pod config (port 8084)
└── service.yaml       # Intern service
```

### 6. Frontend (React)
```
frontend/
├── deployment.yaml    # Pod config (port 3000)
└── service.yaml       # Extern service (NodePort 30000)
```

## 📊 Sammanfattning

```
Total antal filer: 22

Dokumentation:     5 filer  (START_HERE, QUICKSTART, README, CHEATSHEET, STRUCTURE)
Deployment:        2 filer  (deploy-all.sh, namespace.yaml)
MySQL:             3 filer  (pvc, deployment, service)
Microservices:    12 filer  (6 × deployment + service)
```

## ✅ Checklista innan deployment

- [ ] Läst START_HERE.md
- [ ] Läst QUICKSTART.md
- [ ] Aktiverat Kubernetes i Docker Desktop
- [ ] Byggt alla Docker images (6 st)
- [ ] Uppdaterat MySQL lösenord i `mysql/mysql-deployment.yaml`
- [ ] Uppdaterat MySQL credentials i:
  - [ ] `user-service/deployment.yaml`
  - [ ] `message-service/deployment.yaml`
  - [ ] `image-service/deployment.yaml`
- [ ] Kontrollerat att alla portar stämmer
- [ ] Kört `./deploy-all.sh`

## 🎯 Vad behöver du ändra?

### Måste ändras:
1. **MySQL lösenord** i `mysql/mysql-deployment.yaml`
2. **Databas namn** i `mysql/mysql-deployment.yaml`
3. **MySQL credentials** i alla 3 services som använder databas:
   - user-service
   - message-service
   - image-service

### Kanske behöver ändras:
- **Portar** (om dina tjänster kör på andra portar än standard)
- **Environment variables** (om du har fler config-behov)
- **Frontend API URLs** (i frontend/deployment.yaml)

## 📍 Var hittar jag information om...?

| Fråga | Svar finns i |
|-------|--------------|
| Hur kommer jag igång? | START_HERE.md |
| Steg-för-steg installation? | QUICKSTART.md |
| Felsökning och troubleshooting? | README.md |
| Kubectl kommandon? | CHEATSHEET.md |
| Hur systemet hänger ihop? | STRUCTURE.md |
| Hur ändrar jag MySQL-lösenord? | mysql/mysql-deployment.yaml |
| Hur når mina services varandra? | README.md + STRUCTURE.md |

## 🌐 Portar och Access

| Service | Intern Port | Extern Port | URL |
|---------|-------------|-------------|-----|
| MySQL | 3306 | - | mysql-service:3306 (intern) |
| Clinical | 8080 | - | clinical-service:8080 (intern) |
| User | 8081 | - | user-service:8081 (intern) |
| Message | 8082 | - | message-service:8082 (intern) |
| Image | 3001 | - | image-service:3001 (intern) |
| Search | 8084 | - | search-service:8084 (intern) |
| Frontend | 3000 | **30000** | **http://localhost:30000** |

## 💡 Tips

- **Spara CHEATSHEET.md som bokmärke** - du kommer använda den ofta
- **Läs README.md när något går fel** - den har omfattande troubleshooting
- **Använd deploy-all.sh** - det är enklast
- **Kolla logs ofta**: `kubectl logs -f <pod-name> -n patientsystem`

## 🎓 Lär dig mer

1. Börja med START_HERE.md
2. Följ QUICKSTART.md steg för steg
3. Bekanta dig med CHEATSHEET.md
4. Utforska STRUCTURE.md för att förstå arkitekturen
5. Använd README.md som referens

**Lycka till! 🚀**
