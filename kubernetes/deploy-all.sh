#!/bin/bash

echo "🚀 Deploying PatientSystem to Kubernetes..."

# Skapa namespace först
echo "📦 Creating namespace..."
kubectl apply -f namespace.yaml

# Deploy MySQL först (andra tjänster beror på den)
echo "🗄️  Deploying MySQL database..."
kubectl apply -f mysql/

# Vänta på att MySQL ska vara redo
echo "⏳ Waiting for MySQL to be ready..."
kubectl wait --for=condition=ready pod -l app=mysql -n patientsystem --timeout=300s

# Deploy alla microservices
echo "🔧 Deploying microservices..."
kubectl apply -f clinical-service/
kubectl apply -f user-service/
kubectl apply -f message-service/
kubectl apply -f image-service/
kubectl apply -f search-service/
kubectl apply -f frontend/

echo "✅ Deployment complete!"
echo ""
echo "📊 Check status with:"
echo "   kubectl get pods -n patientsystem"
echo ""
echo "🌐 Access frontend at: http://localhost:30000"
echo ""
echo "📝 View logs with:"
echo "   kubectl logs -f <pod-name> -n patientsystem"
