#!/bin/bash

# Get the project root directory from the first argument
PROJECT_ROOT=$1

# Delete Kubernetes configurations
kubectl delete fleet server
kubectl delete fleet proxy
kubectl delete fleet spawn

# Wait for 2 seconds
sleep 2

# Build Docker images
docker build -t server:latest "$PROJECT_ROOT/servers/server/src/main/docker"
docker build -t proxy:latest "$PROJECT_ROOT/servers/proxy/src/main/docker"
docker build -t spawn:latest "$PROJECT_ROOT/servers/spawn/src/main/docker"

# Apply Kubernetes configurations
cd "$PROJECT_ROOT/servers/server/src/main/helm"
kubectl apply -f fleet_local.yaml

cd "$PROJECT_ROOT/servers/proxy/src/main/helm"
kubectl apply -f fleet_local.yaml

cd "$PROJECT_ROOT/servers/spawn/src/main/helm"
kubectl apply -f fleet_local.yaml
