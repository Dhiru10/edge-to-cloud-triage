# Runbook

Operational procedures for the Edge-to-Cloud Triage System.

---

## Local Development

### Start full stack
```bash
cd infra
docker compose up --build
```

### Stop and clean volumes
```bash
docker compose down -v
```

### Run backend only (outside Docker)
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
Requires a running PostgreSQL instance. See `application.yml` for connection defaults.

### Run triage agent only
```bash
cd ai-triage-agent
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
python main.py
```

---

## AWS Deployment

> Requires: AWS CLI configured, Terraform 1.6+, Docker, AWS SAM CLI

### 1. Build and push container images
```bash
# Authenticate to ECR
aws ecr get-login-password --region us-east-1 |   docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Build + push each service
docker build -t edge-triage-backend ./backend
docker tag edge-triage-backend:latest <ecr-repo-uri>/backend:latest
docker push <ecr-repo-uri>/backend:latest

# Repeat for ai-triage-agent and ui
```

### 2. Apply Terraform
```bash
cd infra/terraform
terraform init
terraform plan -var-file="prod.tfvars"
terraform apply -var-file="prod.tfvars"
```

Terraform creates: VPC, subnets, ALB, ECS cluster, Fargate services, RDS instance,
Secrets Manager entries, SNS topic, CloudWatch log groups, EventBridge rule for Lambda.

### 3. Deploy watchdog Lambda
```bash
cd watchdog-lambda
sam build
sam deploy --config-env prod
```

### 4. Run database migrations
Flyway runs automatically when the backend ECS task starts.
To run manually against RDS:
```bash
cd backend
./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://<rds-endpoint>:5432/triagedb
```

---

## Secrets

All secrets are stored in AWS Secrets Manager. Never commit secrets to the repository.

| Secret name | Used by |
|-------------|---------|
| `triage/db-password` | Backend ECS task, Flyway |
| `triage/api-key` | All services (X-Api-Key header) |

To rotate the API key: update the secret in Secrets Manager, then force a new ECS deployment
for the backend and restart the triage agent and watchdog Lambda.

---

## Monitoring

### CloudWatch metrics
- `TriageSystem/StaleEvents` — published by watchdog Lambda every 5 minutes
- ECS service metrics: CPUUtilization, MemoryUtilization (via Container Insights)
- RDS metrics: FreeStorageSpace, DatabaseConnections

### Recommended alarms
| Alarm | Threshold |
|-------|-----------|
| StaleEvents > 5 | Page on-call |
| Backend ECS CPU > 80% for 5 min | Investigate |
| RDS FreeStorageSpace < 5 GB | Expand storage |

---

## Incident Procedures

### Triage agent is stuck / not processing faults
1. Check agent ECS task logs: `aws logs tail /ecs/ai-triage-agent --follow`
2. If the task is crash-looping, check for missing ENV vars (API key, backend URL)
3. The watchdog Lambda will automatically reset stale `processing` events back to `pending`
   within 10 minutes. No manual intervention needed for a brief outage.
4. If the agent is degraded for > 30 minutes, force a new ECS deployment:
   `aws ecs update-service --cluster triage --service ai-triage-agent --force-new-deployment`

### Backend is returning 5xx errors
1. Check ECS task logs: `aws logs tail /ecs/backend --follow`
2. Verify RDS connectivity from within the VPC
3. Check Flyway migration status on startup (logged at INFO level)

### Watchdog Lambda not firing
1. Check EventBridge rule is enabled in the AWS console
2. Check Lambda CloudWatch logs: `/aws/lambda/triage-watchdog`
3. Verify the Lambda execution role has permission to call the backend ALB

---

## Known Limitations

- `raw_log` stored as TEXT in PostgreSQL. For large crash dumps, consider S3 with a signed URL reference.
- Single-region deployment. RDS Multi-AZ covers database availability but not regional failure.
- Static API key auth. Suitable for a controlled deployment; replace with JWT for multi-tenant use.
- Triage agent uses pattern matching, not ML. Confidence scores are heuristic-based.
