variable "project"            {}
variable "environment"        {}
variable "vpc_id"             {}
variable "private_subnet_ids" {}
variable "db_password"        { sensitive = true }

locals {
  name    = "${var.project}-${var.environment}"
  db_name = "triagedb"
  db_user = "triage"
}

resource "aws_security_group" "rds" {
  name   = "${local.name}-rds-sg"
  vpc_id = var.vpc_id

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }
}

resource "aws_db_subnet_group" "main" {
  name       = "${local.name}-db-subnet"
  subnet_ids = var.private_subnet_ids
}

resource "aws_db_instance" "postgres" {
  identifier             = "${local.name}-postgres"
  engine                 = "postgres"
  engine_version         = "15"
  instance_class         = "db.t3.medium"
  allocated_storage      = 20
  storage_type           = "gp3"
  db_name                = local.db_name
  username               = local.db_user
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  multi_az               = true
  skip_final_snapshot    = false
  final_snapshot_identifier = "${local.name}-final"
  tags = { Name = "${local.name}-postgres" }
}

output "db_url" {
  value     = "jdbc:postgresql://${aws_db_instance.postgres.endpoint}/${local.db_name}"
  sensitive = true
}

output "db_endpoint" {
  value     = aws_db_instance.postgres.endpoint
  sensitive = true
}
