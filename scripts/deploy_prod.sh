#!/bin/bash

# Define the base directory
BASE_DIR=$(pwd)

# Define the services
SERVICES=("server" "proxy" "spawn" "limbo")

# Loop through each service and apply the fleet.yaml configuration
for SERVICE in "${SERVICES[@]}"; do
    cd "$BASE_DIR/servers/$SERVICE/src/main/helm"
    kubectl apply -f fleet.yaml
done