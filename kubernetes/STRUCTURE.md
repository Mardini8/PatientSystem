# PatientSystem Kubernetes Struktur

```
kubernetes/
│
├── 📄 namespace.yaml                     # Skapar "patientsystem" namespace
├── 📄 README.md                          # Fullständig dokumentation
├── 📄 CHEATSHEET.md                      # Snabbreferens
├── 🚀 deploy-all.sh                      # Deployment script (kör detta!)
│
├── 🗄️  mysql/                             # MySQL Databas
│   ├── mysql-pvc.yaml                   # Persistent Volume (5GB storage)
│   ├── mysql-deployment.yaml            # MySQL pod
│   └── mysql-service.yaml               # Intern service (port 3306)
│
├── 🔧 clinical-service/                  # Spring Boot tjänst
│   ├── deployment.yaml                  # Pod: clinical-service:latest (port 8080)
│   └── service.yaml                     # Intern service
│
├── 👤 user-service/                      # Spring Boot + MySQL
│   ├── deployment.yaml                  # Pod: user-service:latest (port 8081)
│   └── service.yaml                     # Intern service
│
├── 💬 message-service/                   # Spring Boot + MySQL
│   ├── deployment.yaml                  # Pod: message-service:latest (port 8082)
│   └── service.yaml                     # Intern service
│
├── 🖼️  image-service/                     # Node.js + MySQL
│   ├── deployment.yaml                  # Pod: image-service:latest (port 3001)
│   └── service.yaml                     # Intern service
│
├── 🔍 search-service/                    # Quarkus Reactive
│   ├── deployment.yaml                  # Pod: search-service:latest (port 8084)
│   └── service.yaml                     # Intern service
│
└── 🌐 frontend/                          # React Frontend
    ├── deployment.yaml                  # Pod: frontend:latest (port 3000)
    └── service.yaml                     # EXTERN service (NodePort 30000)
                                         # → Nås på http://localhost:30000
```

## Tjänster som kommunicerar med MySQL

```
┌─────────────────┐
│  user-service   │────┐
│   (port 8081)   │    │
└─────────────────┘    │
                       │
┌─────────────────┐    │    ┌──────────────┐
│ message-service │────┼───→│    MySQL     │
│   (port 8082)   │    │    │  (port 3306) │
└─────────────────┘    │    └──────────────┘
                       │
┌─────────────────┐    │
│  image-service  │────┘
│   (port 3001)   │
└─────────────────┘
```

## Service Communication (exempel)

```
┌──────────────┐
│   Frontend   │
│ (port 3000)  │  http://localhost:30000 (från browser)
└──────┬───────┘
       │
       │ API calls till:
       │ - http://clinical-service:8080
       │ - http://user-service:8081
       │ - http://message-service:8082
       │ - http://image-service:3001
       │ - http://search-service:8084
       ↓
┌──────────────────────────────────────┐
│      Kubernetes Services             │
│  (intern DNS och load balancing)     │
└──────────────────────────────────────┘
```

## Deployment Order

1. **namespace.yaml** → Skapar isolerat namespace
2. **mysql/** → Databas måste vara först (andra tjänster beror på den)
   - PVC (storage) → Deployment → Service
3. **Alla services samtidigt** → Kan deployas parallellt
   - clinical-service
   - user-service (väntar på MySQL)
   - message-service (väntar på MySQL)
   - image-service (väntar på MySQL)
   - search-service
   - frontend

## Storage

```
┌─────────────────────────────┐
│  mysql-pvc                  │
│  (PersistentVolumeClaim)    │
│  5GB storage                │
│  /var/lib/mysql             │
└─────────────────────────────┘
        ↑
        │ mounted in
        │
┌─────────────────────────────┐
│  MySQL Pod                  │
│  Data persisteras även om   │
│  pod startas om             │
└─────────────────────────────┘
```

## Network Flow

```
Browser (localhost:30000)
    ↓
NodePort Service (frontend:30000)
    ↓
Frontend Pod (port 3000)
    ↓
    ├─→ ClusterIP Service (clinical-service:8080)  → Clinical Pod
    ├─→ ClusterIP Service (user-service:8081)      → User Pod → MySQL
    ├─→ ClusterIP Service (message-service:8082)   → Message Pod → MySQL
    ├─→ ClusterIP Service (image-service:3001)     → Image Pod → MySQL
    └─→ ClusterIP Service (search-service:8084)    → Search Pod
```

## Image Requirements

Alla tjänster behöver Docker images byggda lokalt:

```bash
clinical-service:latest
user-service:latest
message-service:latest
image-service:latest
search-service:latest
frontend:latest
```

Bygg med: `docker build -t <service-name>:latest .`

## Resources per Service

| Service | Image | Port | Replicas | DB | Type |
|---------|-------|------|----------|----|----|
| MySQL | mysql:8.0 | 3306 | 1 | - | ClusterIP |
| clinical | clinical-service:latest | 8080 | 1 | - | ClusterIP |
| user | user-service:latest | 8081 | 1 | ✅ | ClusterIP |
| message | message-service:latest | 8082 | 1 | ✅ | ClusterIP |
| image | image-service:latest | 3001 | 1 | ✅ | ClusterIP |
| search | search-service:latest | 8084 | 1 | - | ClusterIP |
| frontend | frontend:latest | 3000 | 1 | - | NodePort |

## Vad händer när du kör deploy-all.sh?

```
1. kubectl apply -f namespace.yaml
   → Skapar namespace "patientsystem"

2. kubectl apply -f mysql/
   → Skapar PVC (storage för databas)
   → Deployer MySQL pod
   → Skapar MySQL service (mysql-service:3306)

3. kubectl wait ... (väntar på MySQL)
   → Väntar tills MySQL är redo att ta emot connections

4. kubectl apply -f alla services/
   → Deployer alla 6 microservices samtidigt
   → Varje service får:
     - En deployment (hanterar pods)
     - En service (DNS + load balancing)

5. Done! 🎉
   → Alla tjänster kör i patientsystem namespace
   → Frontend nås på localhost:30000
```
