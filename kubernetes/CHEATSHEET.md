# Kubernetes Snabbreferens - PatientSystem

## Snabbkommandon

### Deployment
```bash
./deploy-all.sh                              # Deploy allt
kubectl apply -f namespace.yaml              # Skapa namespace
kubectl apply -f mysql/                      # Deploy MySQL
kubectl apply -f clinical-service/           # Deploy en specifik service
```

### Status & Monitoring
```bash
kubectl get pods -n patientsystem                    # Se alla pods
kubectl get services -n patientsystem                # Se alla services
kubectl get all -n patientsystem                     # Se allt
kubectl describe pod <pod-name> -n patientsystem     # Detaljerad info
kubectl logs -f <pod-name> -n patientsystem          # Följ logs
```

### Port Forwarding (för att nå services lokalt)
```bash
kubectl port-forward -n patientsystem service/clinical-service 8080:8080
kubectl port-forward -n patientsystem service/user-service 8081:8081
kubectl port-forward -n patientsystem service/message-service 8082:8082
kubectl port-forward -n patientsystem service/image-service 3001:3001
kubectl port-forward -n patientsystem service/search-service 8084:8084
kubectl port-forward -n patientsystem service/mysql-service 3306:3306
```

### Felsökning
```bash
kubectl get events -n patientsystem --sort-by='.lastTimestamp'     # Se events
kubectl exec -it <pod-name> -n patientsystem -- sh                 # Gå in i pod
kubectl delete pod <pod-name> -n patientsystem                     # Starta om pod
kubectl rollout restart deployment/<name> -n patientsystem         # Starta om deployment
```

### Städning
```bash
kubectl delete namespace patientsystem      # Ta bort ALLT
kubectl delete -f clinical-service/         # Ta bort en specifik service
```

## Tjänster och Portar

| Tjänst | Port | Typ | Databas |
|--------|------|-----|---------|
| clinical-service | 8080 | Spring Boot | - |
| user-service | 8081 | Spring Boot | MySQL |
| message-service | 8082 | Spring Boot | MySQL |
| image-service | 3001 | Node.js | MySQL |
| search-service | 8084 | Quarkus Reactive | - |
| frontend | 3000/30000 | React | - |
| mysql-service | 3306 | MySQL | - |

## Service URLs (inom Kubernetes)

Tjänster når varandra via:
- `http://clinical-service:8080`
- `http://user-service:8081`
- `http://message-service:8082`
- `http://image-service:3001`
- `http://search-service:8084`
- `http://mysql-service:3306`

## Frontend Access

- **Lokalt:** http://localhost:30000

## Vanliga Problem och Lösningar

### "ImagePullBackOff" eller "ErrImagePull"
- **Orsak:** Image finns inte lokalt
- **Lösning:** Bygg imagen igen: `docker build -t <service>:latest .`
- **Kontrollera:** `imagePullPolicy: Never` i deployment.yaml

### Pod i "CrashLoopBackOff"
- **Orsak:** Applikationen kraschar vid start
- **Lösning:** Kolla logs: `kubectl logs <pod-name> -n patientsystem`
- **Vanliga orsaker:**
  - Fel databaskoppling
  - Saknade miljövariabler
  - Port redan används

### Service kan inte nå MySQL
- **Orsak:** MySQL är inte redo eller fel credentials
- **Lösning:**
  1. `kubectl get pods -n patientsystem` - kolla att MySQL är Running
  2. Uppdatera SPRING_DATASOURCE_URL till `jdbc:mysql://mysql-service:3306/...`
  3. Kontrollera username/password

### Kan inte nå frontend på localhost:30000
- **Orsak:** Service typ eller NodePort fel
- **Lösning:** Kontrollera att frontend/service.yaml har `type: NodePort` och `nodePort: 30000`

## Bygg alla images (i projektroot)

```bash
cd clinical-service && docker build -t clinical-service:latest . && cd ..
cd user-service && docker build -t user-service:latest . && cd ..
cd message-service && docker build -t message-service:latest . && cd ..
cd image-service && docker build -t image-service:latest . && cd ..
cd search-service && docker build -t search-service:latest . && cd ..
cd frontend && docker build -t frontend:latest . && cd ..
```

## Nyttiga alias (lägg i ~/.bashrc eller ~/.zshrc)

```bash
alias k='kubectl'
alias kgp='kubectl get pods -n patientsystem'
alias kgs='kubectl get services -n patientsystem'
alias kga='kubectl get all -n patientsystem'
alias klogs='kubectl logs -f -n patientsystem'
alias kdesc='kubectl describe -n patientsystem'
```

Då kan du köra:
```bash
kgp              # istället för kubectl get pods -n patientsystem
klogs <pod>      # istället för kubectl logs -f <pod> -n patientsystem
```
