# PatientSystem Kubernetes Deployment

Detta är Kubernetes-konfigurationen för PatientSystem med 6 microservices och MySQL databas.

## Struktur

```
kubernetes/
├── namespace.yaml                    # Skapar namespace för hela systemet
├── mysql/                            # MySQL databas
│   ├── mysql-pvc.yaml               # Persistent storage
│   ├── mysql-deployment.yaml        # MySQL deployment
│   └── mysql-service.yaml           # MySQL service
├── clinical-service/                # Spring Boot tjänst
│   ├── deployment.yaml
│   └── service.yaml
├── user-service/                    # Spring Boot + MySQL
│   ├── deployment.yaml
│   └── service.yaml
├── message-service/                 # Spring Boot + MySQL
│   ├── deployment.yaml
│   └── service.yaml
├── image-service/                   # Node.js + MySQL (port 3001)
│   ├── deployment.yaml
│   └── service.yaml
├── search-service/                  # Quarkus Reactive (port 8084)
│   ├── deployment.yaml
│   └── service.yaml
└── frontend/                        # React frontend
    ├── deployment.yaml
    └── service.yaml
```

## Förberedelser

### 1. Aktivera Kubernetes i Docker Desktop
1. Öppna Docker Desktop
2. Gå till Settings → Kubernetes
3. Kryssa i "Enable Kubernetes"
4. Klicka "Apply & Restart"
5. Vänta tills det står "Kubernetes is running"

### 2. Bygg dina Docker images
Innan du deployer till Kubernetes måste du bygga alla dina images:

```bash
# I varje service-mapp (clinical-service, user-service, etc.)
cd clinical-service
docker build -t clinical-service:latest .

cd ../user-service
docker build -t user-service:latest .

cd ../message-service
docker build -t message-service:latest .

cd ../image-service
docker build -t image-service:latest .

cd ../search-service
docker build -t search-service:latest .

cd ../frontend
docker build -t frontend:latest .
```

### 3. Uppdatera konfiguration
**VIKTIGT:** Innan du deployer, öppna och uppdatera följande:

#### MySQL credentials (i `mysql/mysql-deployment.yaml`):
```yaml
- name: MYSQL_ROOT_PASSWORD
  value: "DITT_LÖSENORD_HÄR"
- name: MYSQL_DATABASE
  value: "DIN_DATABAS_HÄR"
```

#### Database connections (i service deployments):
- `user-service/deployment.yaml`
- `message-service/deployment.yaml`
- `image-service/deployment.yaml`

Uppdatera:
- Database namn
- Username
- Password
- Portar (om de skiljer sig från 8080, 8081, 8082, 3001, 8084, 3000)

## Deployment

### Snabb start (använd scriptet):
```bash
cd kubernetes
./deploy-all.sh
```

### Manuell deployment:
```bash
# 1. Skapa namespace
kubectl apply -f namespace.yaml

# 2. Deploy MySQL först
kubectl apply -f mysql/

# 3. Vänta på MySQL (viktig!)
kubectl wait --for=condition=ready pod -l app=mysql -n patientsystem --timeout=300s

# 4. Deploy alla services
kubectl apply -f clinical-service/
kubectl apply -f user-service/
kubectl apply -f message-service/
kubectl apply -f image-service/
kubectl apply -f search-service/
kubectl apply -f frontend/
```

## Verifiera deployment

### Kolla status på alla pods:
```bash
kubectl get pods -n patientsystem
```

Du bör se något liknande:
```
NAME                                READY   STATUS    RESTARTS   AGE
mysql-xxx                           1/1     Running   0          2m
clinical-service-xxx                1/1     Running   0          1m
user-service-xxx                    1/1     Running   0          1m
message-service-xxx                 1/1     Running   0          1m
image-service-xxx                   1/1     Running   0          1m
search-service-xxx                  1/1     Running   0          1m
frontend-xxx                        1/1     Running   0          1m
```

### Kolla services:
```bash
kubectl get services -n patientsystem
```

### Kolla logs för en specifik service:
```bash
# Lista pods först
kubectl get pods -n patientsystem

# Visa logs
kubectl logs -f <pod-name> -n patientsystem

# Exempel:
kubectl logs -f user-service-7d9f8b6c4-abc12 -n patientsystem
```

## Åtkomst till tjänster

### Frontend (React):
```
http://localhost:30000
```

### Andra tjänster (från din lokala dator):
Du kan port-forward till vilken tjänst som helst:

```bash
# Clinical Service
kubectl port-forward -n patientsystem service/clinical-service 8080:8080

# User Service
kubectl port-forward -n patientsystem service/user-service 8081:8081

# Message Service
kubectl port-forward -n patientsystem service/message-service 8082:8082

# Image Service
kubectl port-forward -n patientsystem service/image-service 3001:3001

# Search Service
kubectl port-forward -n patientsystem service/search-service 8084:8084

# MySQL (för debugging)
kubectl port-forward -n patientsystem service/mysql-service 3306:3306
```

## Felsökning

### Pod startar inte?
```bash
# Se detaljerad info
kubectl describe pod <pod-name> -n patientsystem

# Se events
kubectl get events -n patientsystem --sort-by='.lastTimestamp'
```

### Database connection issues?
```bash
# Kolla MySQL logs
kubectl logs -f $(kubectl get pod -l app=mysql -n patientsystem -o jsonpath='{.items[0].metadata.name}') -n patientsystem

# Testa anslutning från en pod
kubectl exec -it <service-pod-name> -n patientsystem -- sh
# Sedan inne i pod:
ping mysql-service
```

### Image pull error?
Kontrollera att `imagePullPolicy: Never` finns i alla deployments (för lokala images).

### Starta om en service:
```bash
kubectl rollout restart deployment/<service-name> -n patientsystem

# Exempel:
kubectl rollout restart deployment/user-service -n patientsystem
```

## Ta bort allt

```bash
# Ta bort hela namespacet (tar bort allt inuti det)
kubectl delete namespace patientsystem
```

## Viktiga skillnader från Docker

| Docker | Kubernetes |
|--------|------------|
| Container namn: `mysql-container` | Service namn: `mysql-service` |
| Network: `patientsystem-net` | Namespace: `patientsystem` (automatiskt nätverk) |
| `docker run -p 3000:3000` | Service med `type: NodePort` och `nodePort: 30000` |
| Miljövariabler i docker-compose | Miljövariabler i deployment.yaml |

## Service Discovery

I Kubernetes kan tjänster nå varandra via DNS:
- `http://mysql-service:3306` (från samma namespace)
- `http://user-service:8081` (från samma namespace)
- `http://clinical-service.patientsystem.svc.cluster.local:8080` (fullständigt DNS-namn)

## Nästa steg

1. ✅ Aktivera Kubernetes i Docker Desktop
2. ✅ Bygg alla Docker images
3. ✅ Uppdatera credentials och portar
4. ✅ Kör `./deploy-all.sh`
5. ✅ Verifiera med `kubectl get pods -n patientsystem`
6. ✅ Öppna frontend på http://localhost:30000

## Tips

- Använd `kubectl get all -n patientsystem` för att se allt på en gång
- Använd `kubectl logs -f <pod> -n patientsystem` för att följa logs i realtid
- Använd `kubectl describe` för att debugga problem
- Spara dina Kubernetes-filer i version control (git)

## För KTH Cloud senare

Dessa filer kommer att fungera i KTH Cloud också! Du behöver bara:
1. Pusha dina images till ett registry (t.ex. Docker Hub eller KTH's registry)
2. Ändra `imagePullPolicy: Never` till `imagePullPolicy: Always`
3. Uppdatera image paths till registry paths
