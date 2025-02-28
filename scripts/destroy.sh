#!/bin/bash

# Stop Kubernetes configurations
kubectl delete fleet proxy
kubectl delete fleet oneblock-spawn
kubectl delete fleet oneblock-server
kubectl delete fleet hub