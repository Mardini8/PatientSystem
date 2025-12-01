# 🚀 Snabbstart - PatientSystem i Kubernetes

## Steg 1: Förberedelser (10 min)

### ✅ Aktivera Kubernetes i Docker Desktop
1. Öppna **Docker Desktop**
2. Klicka på ⚙️ **Settings** (uppe till höger)
3. Välj **Kubernetes** i menyn till vänster
4. ✅ Kryssa i **"Enable Kubernetes"**
5. Klicka **"Apply & Restart"**
6. ⏳ Vänta tills det står **"Kubernetes is running"** (kan ta 2-5 minuter)

### ✅ Verifiera att Kubernetes fungerar
Öppna en terminal och kör:
```bash
kubectl version --short
kubectl get nodes
```

Du bör se något liknande:
```
Client Version: v1.28.x
Server Version: v1.28.x

NAME             STATUS   ROLES           AGE
docker-desktop   Ready    control-plane   5m
```

## Steg 2: Bygg Docker Images (5 min)

Från din **PatientSystem** projektmapp, bygg alla images:

```bash
# Clinical Service
cd clinical-service
docker build -t clinical-service:latest .
cd ..

# User Service
cd user-service
docker build -t user-service:latest .
cd ..

# Message Service
cd message-service
docker build -t message-service:latest .
cd ..

# Image Service (Node.js)
cd image-service
docker build -t image-service:latest .
cd ..

# Search Service (Quarkus)
cd search-service
docker build -t search-service:latest .
cd ..

# Frontend (React)
cd frontend
docker build -t frontend:latest .
cd ..
```

### ✅ Verifiera att alla images finns:
```bash
docker images | grep -E "clinical-service|user-service|message-service|image-service|search-service|frontend"
```

## Steg 3: Uppdatera Kubernetes konfiguration (3 min)

### 📝 Uppdatera MySQL credentials

Öppna `kubernetes/mysql/mysql-deployment.yaml` och ändra:

```yaml
env:
- name: MYSQL_ROOT_PASSWORD
  value: "DITT_LÖSENORD_HÄR"  # <-- ÄNDRA!
- name: MYSQL_DATABASE
  value: "patientsystem"        # <-- ÄNDRA till ditt databas-namn!
```

### 📝 Uppdatera databas-anslutningar

För varje service som använder MySQL (**user-service**, **message-service**, **image-service**):

**För Spring Boot services** (user och message):
Öppna `kubernetes/user-service/deployment.yaml` och `kubernetes/message-service/deployment.yaml`:

```yaml
env:
- name: SPRING_DATASOURCE_URL
  value: "jdbc:mysql://mysql-service:3306/DITT_DATABAS_NAMN"  # <-- ÄNDRA!
- name: SPRING_DATASOURCE_USERNAME
  value: "root"  # <-- ÄNDRA om du använder annan user
- name: SPRING_DATASOURCE_PASSWORD
  value: "DITT_LÖSENORD"  # <-- ÄNDRA!
```

**För Node.js service** (image-service):
Öppna `kubernetes/image-service/deployment.yaml`:

```yaml
env:
- name: DB_HOST
  value: "mysql-service"
- name: DB_NAME
  value: "patientsystem"  # <-- ÄNDRA!
- name: DB_USER
  value: "root"  # <-- ÄNDRA!
- name: DB_PASSWORD
  value: "DITT_LÖSENORD"  # <-- ÄNDRA!
```

### 📝 Uppdatera portar (om de skiljer sig)

Kontrollera att portarna stämmer i varje `deployment.yaml`:
- clinical-service: **8080** ✅
- user-service: **8081** ✅
- message-service: **8082** ✅
- image-service: **3001** ✅
- search-service: **8084** ✅
- frontend: **3000** ✅

## Steg 4: Deploya till Kubernetes! (2 min)

```bash
cd kubernetes
chmod +x deploy-all.sh
./deploy-all.sh
```

Du bör se:
```
🚀 Deploying PatientSystem to Kubernetes...
📦 Creating namespace...
🗄️  Deploying MySQL database...
⏳ Waiting for MySQL to be ready...
🔧 Deploying microservices...
✅ Deployment complete!

📊 Check status with:
   kubectl get pods -n patientsystem

🌐 Access frontend at: http://localhost:30000
```

## Steg 5: Verifiera deployment (1 min)

```bash
kubectl get pods -n patientsystem
```

**Vänta tills alla pods visar `Running` och `1/1` Ready:**
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

⚠️ **Om någon pod visar fel** (CrashLoopBackOff, Error, ImagePullBackOff):
```bash
# Se vad som är fel:
kubectl describe pod <pod-name> -n patientsystem

# Se logs:
kubectl logs <pod-name> -n patientsystem
```

Vanliga problem:
- **ImagePullBackOff** → Docker imagen finns inte, bygg om den
- **CrashLoopBackOff** → Kolla logs, ofta databas-connection fel
- **Pending** → Vänta lite, Kubernetes startar poden

## Steg 6: Testa applikationen! 🎉

### Öppna din frontend:
```
http://localhost:30000
```

### Testa backend services (med port-forward):

**Öppna nya terminal-fönster för varje:**

```bash
# User Service
kubectl port-forward -n patientsystem service/user-service 8081:8081
# Nu kan du nå: http://localhost:8081

# Message Service  
kubectl port-forward -n patientsystem service/message-service 8082:8082
# Nu kan du nå: http://localhost:8082

# Image Service
kubectl port-forward -n patientsystem service/image-service 3001:3001
# Nu kan du nå: http://localhost:3001

# Clinical Service
kubectl port-forward -n patientsystem service/clinical-service 8080:8080
# Nu kan du nå: http://localhost:8080

# Search Service
kubectl port-forward -n patientsystem service/search-service 8084:8084
# Nu kan du nå: http://localhost:8084
```

## 🎓 Användbara kommandon

### Se allt som körs:
```bash
kubectl get all -n patientsystem
```

### Följ logs i realtid:
```bash
kubectl logs -f <pod-name> -n patientsystem

# Exempel:
kubectl logs -f user-service-7d9f8b6c4-abc12 -n patientsystem
```

### Starta om en service:
```bash
kubectl rollout restart deployment/user-service -n patientsystem
```

### Ta bort allt:
```bash
kubectl delete namespace patientsystem
```

## ❓ Behöver du hjälp?

Se `README.md` för fullständig dokumentation
Se `CHEATSHEET.md` för snabbreferens
Se `STRUCTURE.md` för visuell översikt

## 🎯 Nästa steg

Nu kör din PatientSystem i Kubernetes! 🎉

Några saker du kan göra härnäst:
- Lägg till fler replicas för high availability
- Konfigurera Ingress för bättre routing
- Lägg till health checks och readiness probes
- Sätt upp monitoring med Prometheus/Grafana
- Förbereda för deployment till KTH Cloud

---

**Lycka till! 🚀**
