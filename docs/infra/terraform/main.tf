terraform {
  required_version = ">= 1.6"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

module "networking" {
  source      = "./modules/networking"
  project     = var.project
  environment = var.environment
}

module "rds" {
  source             = "./modules/rds"
  project            = var.project
  environment        = var.environment
  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
  db_password        = var.db_password
}

module "ecs" {
  source             = "./modules/ecs"
  project            = var.project
  environment        = var.environment
  vpc_id             = module.networking.vpc_id
  public_subnet_ids  = module.networking.public_subnet_ids
  private_subnet_ids = module.networking.private_subnet_ids
  db_url             = module.rds.db_url
  db_password        = var.db_password
  api_key            = var.api_key
  backend_image      = var.backend_image
  triage_agent_image = var.triage_agent_image
  ui_image           = var.ui_image
}
