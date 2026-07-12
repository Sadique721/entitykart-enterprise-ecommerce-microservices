# Contributing to EntityKart

Thank you for your interest in contributing! Please read these guidelines before submitting a PR.

## Code of Conduct
Be respectful and constructive. We welcome all experience levels.

## Development Setup

### Prerequisites
- Java 17 (Temurin recommended)
- Docker Desktop
- Node.js 18+ (for desktop app)

### Quick Start
```bash
# Clone
git clone https://github.com/<your-org>/entitykart.git
cd entitykart

# Copy and fill environment variables
cp .env.example .env
# Edit .env with your Aiven DB creds, JWT secret, mail creds, etc.

# Start all services
docker compose up -d

# Frontend is live at:
# http://localhost:9001
```

## Branch Strategy
- `main` — production-ready code only
- `develop` — integration branch
- `feature/<name>` — new features
- `fix/<issue-id>` — bug fixes

## Commit Message Format
We follow [Conventional Commits](https://www.conventionalcommits.org/):
```
feat(user-service): add refresh-token endpoint
fix(nginx): restrict /eureka/ to internal network
security(docker): remove hardcoded MAIL_PASSWORD
docs: update CHANGELOG for v2.0.0
test(cart): add CartService unit tests
```

## Pull Request Guidelines
1. Create a branch from `develop`
2. Write/update unit tests for your changes
3. Run all builds: `.\build_all.bat` (Windows) or `./build_all.sh` (Linux)
4. Run `docker compose up -d --build` and verify all containers are healthy
5. Submit PR against `develop`

## Environment Variables Reference
See [`.env.example`](.env.example) for all required variables.

## Security Reporting
Do NOT open public issues for security vulnerabilities.  
Email: security@entitykart.com
