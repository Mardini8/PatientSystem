#!/bin/bash

echo "🚀 Starting port-forwarding for PatientSystem..."

# Kill any existing port-forwards
pkill -f "kubectl port-forward"

# Start port-forwarding for all services
kubectl port-forward -n patientsystem service/frontend 3000:80 &
echo "✅ Frontend: http://localhost:3000"

kubectl port-forward -n patientsystem service/clinical-service 8082:8082 &
echo "✅ Clinical Service: http://localhost:8082"

kubectl port-forward -n patientsystem service/user-service 8081:8081 &
echo "✅ User Service: http://localhost:8081"

kubectl port-forward -n patientsystem service/message-service 8083:8083 &
echo "✅ Message Service: http://localhost:8083"

kubectl port-forward -n patientsystem service/image-service 3001:3001 &
echo "✅ Image Service: http://localhost:3001"

kubectl port-forward -n patientsystem service/search-service 8084:8084 &
echo "✅ Search Service: http://localhost:8084"

echo ""
echo "🎉 All services are now accessible!"
echo "📝 To stop: pkill -f 'kubectl port-forward'"
echo ""
echo "Press Ctrl+C to stop all port-forwards"

# Wait for user to press Ctrl+C
wait
