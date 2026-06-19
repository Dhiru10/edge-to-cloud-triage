output "alb_dns_name" {
  value       = module.ecs.alb_dns_name
  description = "Public DNS name of the Application Load Balancer"
}

output "rds_endpoint" {
  value       = module.rds.db_endpoint
  description = "RDS PostgreSQL endpoint"
  sensitive   = true
}
