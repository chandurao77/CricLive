# Deploy Skill

## Overview
Step-by-step deployment procedures for all environments.
Claude will automatically load this context when you mention deploy, release, rollout, or CI/CD tasks.

## Environments

| Env | Cluster | Namespace | Auto-deploy |
|---|---|---|---|
| `local` | Docker Compose | — | on `npm run dev` / `./gradlew bootRun` |
| `staging` | AWS EKS staging | `staging` | on merge to `develop` |
| `production` | AWS EKS production | `production` | manual trigger only |

---

## Pre-Deployment Checklist
- [ ] All CI checks green (tests, lint, security scan)
- [ ] Docker image built and tagged with commit SHA
- [ ] Secrets updated in AWS Secrets Manager if changed
- [ ] DB migrations reviewed, tested locally, and backward-compatible
- [ ] Feature flags set correctly for target environment
- [ ] Rollback plan identified (previous image tag noted)
- [ ] Monitoring dashboards open and baseline captured

---

## Local Development

```bash
# Start all services via Docker Compose
docker compose up -d

# Or run individually
./gradlew bootRun          # Backend on :8080
npm run dev                # Frontend on :5173

# Tear down
docker compose down -v     # -v removes volumes (fresh DB)
```

---

## Deploy to Staging

```bash
# 1. Build and push image
export COMMIT_SHA=$(git rev-parse --short HEAD)
docker build -t $ECR_REGISTRY/app:$COMMIT_SHA .
docker push $ECR_REGISTRY/app:$COMMIT_SHA

# 2. Run DB migrations first
kubectl run migrations --image=$ECR_REGISTRY/app:$COMMIT_SHA \
  --restart=Never -n staging \
  --env="SPRING_PROFILES_ACTIVE=staging" \
  -- java -jar app.jar --spring.flyway.target=latest
kubectl wait --for=condition=complete job/migrations -n staging --timeout=120s

# 3. Deploy via Helm
helm upgrade --install app ./infra/helm \
  --namespace staging \
  --set image.tag=$COMMIT_SHA \
  --values ./infra/helm/values-staging.yaml \
  --atomic --timeout 3m

# 4. Verify rollout
kubectl rollout status deployment/app -n staging
kubectl get pods -n staging -l app=app
```

---

## Deploy to Production

> ⚠️ Production deploys require Slack notification and a JIRA ticket in DONE state.

```bash
export COMMIT_SHA=$(git rev-parse --short HEAD)

# 1. Notify team
./scripts/notify-deploy.sh --env production --sha $COMMIT_SHA

# 2. Run migrations
kubectl run migrations-prod --image=$ECR_REGISTRY/app:$COMMIT_SHA \
  --restart=Never -n production \
  --env="SPRING_PROFILES_ACTIVE=production" \
  -- java -jar app.jar --spring.flyway.target=latest
kubectl wait --for=condition=complete job/migrations-prod -n production --timeout=180s

# 3. Rolling deploy
helm upgrade app ./infra/helm \
  --namespace production \
  --set image.tag=$COMMIT_SHA \
  --values ./infra/helm/values-production.yaml \
  --atomic --timeout 5m

# 4. Smoke test
curl -sf https://app.example.com/actuator/health | jq .status
curl -sf https://app.example.com/api/v1/ping
```

---

## Rollback

```bash
# Immediate rollback to previous release
helm rollback app -n production

# Rollback to a specific revision
helm history app -n production          # list revisions
helm rollback app 3 -n production       # roll back to revision 3

# Verify
kubectl rollout status deployment/app -n production
```

---

## Post-Deploy Verification

1. **Pod health** — `kubectl get pods -n production` — all pods `Running`
2. **Health endpoint** — `curl https://app.example.com/actuator/health`
3. **Key flows** — `npm run test:smoke -- --env production`
4. **Grafana** — check Error Rate and P99 latency dashboards
5. **Logs** — `kubectl logs -l app=app -n production --tail=100 -f`
6. **Slack** — post completion to `#deployments`

---

## Common Failure Scenarios

| Symptom | Likely Cause | Fix |
|---|---|---|
| Pods stuck in `Pending` | Insufficient cluster capacity | Check node group scaling policy |
| `CrashLoopBackOff` | Bad env var or missing secret | `kubectl describe pod <name> -n production` |
| Migration timeout | Long-running migration on large table | Use `ADD COLUMN` with default, not rewrite |
| `ImagePullBackOff` | Image not pushed or wrong tag | Verify ECR push, check image.tag value |
| `--atomic` rollback triggered | Health checks failing | Check app logs; previous release auto-restored |

---

## Useful Aliases (add to ~/.zshrc)

```bash
alias k='kubectl'
alias kprod='kubectl -n production'
alias kstage='kubectl -n staging'
alias klogs='kubectl logs -l app=app -n production --tail=200 -f'
alias kpods='kubectl get pods -n production -o wide'
```