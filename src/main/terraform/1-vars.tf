variable "linode_token" {
  sensitive = true
  type = string
  description = "Token for linode account"
}

variable "region" {
  description = "Linode region for the cluster"
  type        = string
  default     = "us-central"  # Dallas region
}

variable "k8s_version" {
  description = "Kubernetes version"
  type        = string
  default     = "1.31"
}

variable "node_type" {
  description = "Linode instance type for the node pools"
  type        = string
  default     = "g6-dedicated-4"  # Dedicated 8GB plan (8GB RAM, 2 vCPUs)
}

variable "node_count" {
  description = "Number of nodes per pool"
  type        = number
  default     = 3
}