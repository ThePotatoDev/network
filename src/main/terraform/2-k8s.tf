resource "linode_lke_cluster" "minecraft_dev" {
  label       = "minecraft-dev"
  region      = var.region
  k8s_version = var.k8s_version

  pool {
    type  = var.node_type
    count = var.node_count
  }
}

output "kubeconfig" {
  value     = linode_lke_cluster.minecraft_dev.kubeconfig
  sensitive = true
}