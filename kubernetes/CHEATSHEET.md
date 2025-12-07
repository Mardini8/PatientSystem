# Kubernetes Snabbreferens - PatientSystem

## Uppdatera en deployment
```
cd <service-name>
./mvnw clean package -DskipTests # Bara backend service tjänster
docker build -t <service-name>:latest .
kubectl rollout restart deployment/<service-name> -n patientsystem
```


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
