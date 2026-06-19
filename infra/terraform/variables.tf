variable "aws_region" {
  default = "us-east-1"
}

variable "project" {
  default = "edge-triage"
}

variable "environment" {
  default = "prod"
}

variable "db_password" {
  sensitive = true
}

variable "api_key" {
  sensitive = true
}

variable "backend_image" {
  description = "ECR image URI for the Spring Boot backend"
}

variable "triage_agent_image" {
  description = "ECR image URI for the Python triage agent"
}

variable "ui_image" {
  description = "ECR image URI for the React UI"
}
