#!/bin/bash

# Stop Kubernetes configurations
kubectl delete fleet server
kubectl delete fleet proxy
kubectl delete fleet spawn
kubectl delete fleet hub