# PatientSystem Kubernetes - Sammanfattning

## 📦 Vad du har fått

En komplett Kubernetes-konfiguration för ditt PatientSystem med:

### Struktur
```
kubernetes/
├── 📘 QUICKSTART.md         ← BÖRJA HÄR! Steg-för-steg guide
├── 📘 README.md              ← Fullständig dokumentation
├── 📘 CHEATSHEET.md          ← Snabbreferens för kommandon
├── 📘 STRUCTURE.md           ← Visuell översikt av systemet
│
├── 📄 namespace.yaml         ← Kubernetes namespace
├── 🚀 deploy-all.sh          ← Kör detta för att deploya!
│
└── Kubernetes configs för alla 7 komponenter:
    ├── mysql/                (Databas med persistent storage)
    ├── clinical-service/     (Spring Boot, port 8080)
    ├── user-service/         (Spring Boot + MySQL, port 8081)
    ├── message-service/      (Spring Boot + MySQL, port 8082)
    ├── image-service/        (Node.js + MySQL, port 3001)
    ├── search-service/       (Quarkus Reactive, port 8084)
    └── frontend/             (React, port 3000, extern port 30000)
```

## 🎯 Dina 6 microservices

| # | Service | Teknologi | Port | Databas |
|---|---------|-----------|------|---------|
| 1 | clinical-service | Spring Boot | 8080 | - |
| 2 | user-service | Spring Boot | 8081 | MySQL ✅ |
| 3 | message-service | Spring Boot | 8082 | MySQL ✅ |
| 4 | image-service | Node.js | 3001 | MySQL ✅ |
| 5 | search-service | Quarkus Reactive | 8084 | - |
| 6 | frontend | React | 3000/30000 | - |

Plus:
- MySQL databas (port 3306) med persistent storage

## 🚀 Hur du kommer igång

### 1. Läs QUICKSTART.md
Börja med **QUICKSTART.md** - det är en steg-för-steg guide på svenska som tar dig genom:
- Aktivera Kubernetes i Docker Desktop
- Bygga dina Docker images
- Uppdatera konfiguration
- Deploya systemet
- Verifiera att allt fungerar

### 2. Packa upp filerna
```bash
# Packa upp zip-filen i din PatientSystem mapp
cd /path/to/PatientSystem
unzip kubernetes.zip
```

### 3. Bygg dina images
```bash
# Från varje service-mapp
docker build -t clinical-service:latest .
docker build -t user-service:latest .
docker build -t message-service:latest .
docker build -t image-service:latest .
docker build -t search-service:latest .
docker build -t frontend:latest .
```

### 4. Uppdatera configs
Öppna och uppdatera:
- `mysql/mysql-deployment.yaml` → Databas namn och lösenord
- `user-service/deployment.yaml` → Databas connection
- `message-service/deployment.yaml` → Databas connection  
- `image-service/deployment.yaml` → Databas connection

### 5. Deploya!
```bash
cd kubernetes
./deploy-all.sh
```

### 6. Testa
Öppna http://localhost:30000 i din webbläsare!

## 📚 Dokumentation

| Fil | Syfte | När använda |
|-----|-------|-------------|
| **QUICKSTART.md** | Steg-för-steg guide | När du ska sätta upp första gången |
| **README.md** | Fullständig docs | När du behöver djup förståelse |
| **CHEATSHEET.md** | Snabba kommandon | Daglig användning |
| **STRUCTURE.md** | Visuell översikt | När du vill förstå arkitekturen |

## ⚙️ Viktiga skillnader från Docker

| Docker | Kubernetes |
|--------|------------|
| `docker network create patientsystem-net` | Inte nödvändigt - automatiskt nätverk |
| Container namn: `mysql-container` | Service namn: `mysql-service` |
| `docker run -p 3000:3000` | NodePort service på port 30000 |
| Miljövariabler i Dockerfile | Miljövariabler i deployment.yaml |

## 🔧 Service Discovery

Dina tjänster kan nå varandra via DNS:
```
http://mysql-service:3306
http://clinical-service:8080
http://user-service:8081
http://message-service:8082
http://image-service:3001
http://search-service:8084
```

## 💡 Viktiga noteringar

### MySQL Anslutning
Ändra från:
```
jdbc:mysql://mysql-container:3306/patientsystem
```

Till:
```
jdbc:mysql://mysql-service:3306/patientsystem
```

### Image Pull Policy
Alla deployments använder `imagePullPolicy: Never` för lokala images. Detta måste ändras till `Always` när du deployer till KTH Cloud.

### Persistent Storage
MySQL använder en PersistentVolumeClaim (PVC) på 5GB. Detta betyder att din data kommer att överleva även om MySQL-poden startas om.

## 🎓 Nästa steg efter deployment

1. **Lär dig grundläggande kubectl-kommandon** (se CHEATSHEET.md)
2. **Lägg till health checks** (readiness/liveness probes)
3. **Konfigurera resource limits** (CPU/minne)
4. **Sätt upp Ingress** för bättre routing
5. **Förbered för KTH Cloud** (update image registry)

## 🆘 Vanliga problem

### Pod startar inte?
```bash
kubectl describe pod <pod-name> -n patientsystem
kubectl logs <pod-name> -n patientsystem
```

### Kan inte nå frontend?
- Kontrollera att porten är 30000: http://localhost:30000
- Kolla att frontend-pod kör: `kubectl get pods -n patientsystem`

### MySQL connection error?
- Vänta tills MySQL är helt startad (kan ta 1-2 minuter)
- Kontrollera credentials i deployment.yaml-filerna
- Verifiera att service namn är `mysql-service`

## 🎉 Grattis!

Du har nu en professionell Kubernetes-setup för ditt PatientSystem!

Nästa gång du behöver deploya om:
```bash
cd kubernetes
./deploy-all.sh
```

Det är allt! 🚀

---

**Har du frågor?** Se de andra dokumentationsfilerna eller kör:
```bash
kubectl get all -n patientsystem  # Se allt som körs
kubectl logs -f <pod> -n patientsystem  # Följ logs
```
