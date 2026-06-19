variable "project"            {}
variable "environment"        {}
variable "vpc_id"             {}
variable "public_subnet_ids"  {}
variable "private_subnet_ids" {}
variable "db_url"             { sensitive = true }
variable "db_password"        { sensitive = true }
variable "api_key"            { sensitive = true }
variable "backend_image"      {}
variable "triage_agent_image" {}
variable "ui_image"           {}

locals {
  name = "${var.project}-${var.environment}"
}

# ── Cluster ───────────────────────────────────────────────────────────────────
resource "aws_ecs_cluster" "main" {
  name = "${local.name}-cluster"
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

# ── ALB ───────────────────────────────────────────────────────────────────────
resource "aws_security_group" "alb" {
  name   = "${local.name}-alb-sg"
  vpc_id = var.vpc_id
  ingress { from_port = 80;  to_port = 80;  protocol = "tcp"; cidr_blocks = ["0.0.0.0/0"] }
  ingress { from_port = 443; to_port = 443; protocol = "tcp"; cidr_blocks = ["0.0.0.0/0"] }
  egress  { from_port = 0;   to_port = 0;   protocol = "-1";  cidr_blocks = ["0.0.0.0/0"] }
}

resource "aws_lb" "main" {
  name               = "${local.name}-alb"
  internal           = false
  load_balancer_type = "application"
  subnets            = var.public_subnet_ids
  security_groups    = [aws_security_group.alb.id]
}

resource "aws_lb_target_group" "backend" {
  name        = "${local.name}-backend"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"
  health_check { path = "/actuator/health"; matcher = "200" }
}

resource "aws_lb_target_group" "ui" {
  name        = "${local.name}-ui"
  port        = 80
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"
  health_check { path = "/"; matcher = "200" }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.ui.arn
  }
}

resource "aws_lb_listener_rule" "api" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 10
  condition { path_pattern { values = ["/api/*"] } }
  action { type = "forward"; target_group_arn = aws_lb_target_group.backend.arn }
}

# ── IAM task execution role ───────────────────────────────────────────────────
resource "aws_iam_role" "ecs_exec" {
  name = "${local.name}-ecs-exec"
  assume_role_policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [{ Effect = "Allow"; Principal = { Service = "ecs-tasks.amazonaws.com" }; Action = "sts:AssumeRole" }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_exec" {
  role       = aws_iam_role.ecs_exec.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# ── Backend service ───────────────────────────────────────────────────────────
resource "aws_security_group" "backend" {
  name   = "${local.name}-backend-sg"
  vpc_id = var.vpc_id
  ingress { from_port = 8080; to_port = 8080; protocol = "tcp"; security_groups = [aws_security_group.alb.id] }
  egress  { from_port = 0;    to_port = 0;    protocol = "-1";  cidr_blocks = ["0.0.0.0/0"] }
}

resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/${local.name}/backend"
  retention_in_days = 14
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "${local.name}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.ecs_exec.arn
  container_definitions = jsonencode([{
    name      = "backend"
    image     = var.backend_image
    portMappings = [{ containerPort = 8080 }]
    environment = [
      { name = "SPRING_DATASOURCE_URL",      value = var.db_url },
      { name = "SPRING_DATASOURCE_USERNAME", value = "triage" },
      { name = "SPRING_DATASOURCE_PASSWORD", value = var.db_password },
      { name = "APP_API_KEY",                value = var.api_key },
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options   = { "awslogs-group" = aws_cloudwatch_log_group.backend.name; "awslogs-region" = "us-east-1"; "awslogs-stream-prefix" = "ecs" }
    }
  }])
}

resource "aws_ecs_service" "backend" {
  name            = "${local.name}-backend"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = 1
  launch_type     = "FARGATE"
  network_configuration {
    subnets         = var.private_subnet_ids
    security_groups = [aws_security_group.backend.id]
  }
  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "backend"
    container_port   = 8080
  }
  depends_on = [aws_lb_listener_rule.api]
}

# ── Triage agent (private, no inbound) ───────────────────────────────────────
resource "aws_cloudwatch_log_group" "agent" {
  name              = "/ecs/${local.name}/agent"
  retention_in_days = 14
}

resource "aws_ecs_task_definition" "agent" {
  family                   = "${local.name}-agent"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.ecs_exec.arn
  container_definitions = jsonencode([{
    name  = "ai-triage-agent"
    image = var.triage_agent_image
    environment = [
      { name = "BACKEND_URL", value = "http://${aws_lb.main.dns_name}" },
      { name = "API_KEY",     value = var.api_key },
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options   = { "awslogs-group" = aws_cloudwatch_log_group.agent.name; "awslogs-region" = "us-east-1"; "awslogs-stream-prefix" = "ecs" }
    }
  }])
}

resource "aws_security_group" "agent" {
  name   = "${local.name}-agent-sg"
  vpc_id = var.vpc_id
  egress { from_port = 0; to_port = 0; protocol = "-1"; cidr_blocks = ["0.0.0.0/0"] }
}

resource "aws_ecs_service" "agent" {
  name            = "${local.name}-agent"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.agent.arn
  desired_count   = 1
  launch_type     = "FARGATE"
  network_configuration {
    subnets         = var.private_subnet_ids
    security_groups = [aws_security_group.agent.id]
  }
  depends_on = [aws_ecs_service.backend]
}

output "alb_dns_name" { value = aws_lb.main.dns_name }
