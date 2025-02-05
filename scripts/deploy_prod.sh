#!/bin/bash

# Get the current directory where the terminal was opened
PROJECT_ROOT="$(pwd)"

# Wait for 2 seconds
sleep 2

# Apply Kubernetes configurations
cd "$PROJECT_ROOT/servers/server/src/main/helm"
kubectl apply -f fleet_prod.yaml

cd "$PROJECT_ROOT/servers/proxy/src/main/helm"
kubectl apply -f fleet_prod.yaml

cd "$PROJECT_ROOT/servers/spawn/src/main/helm"
kubectl apply -f fleet_prod.yaml

cd "$PROJECT_ROOT/servers/limbo/src/main/helm"
kubectl apply -f fleet_prod.yaml