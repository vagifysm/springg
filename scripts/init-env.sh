#!/usr/bin/env bash
# Creates .env from .env.example and fills in freshly generated secrets.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -f .env ]; then
  echo ".env already exists - leaving it untouched."
  exit 0
fi
command -v openssl >/dev/null 2>&1 || { echo "openssl is required to generate secrets." >&2; exit 1; }

DB_PASSWORD="$(openssl rand -hex 16)"
JWT_SECRET="$(openssl rand -base64 48 | tr -d '\n')"
ADMIN_PASSWORD="$(openssl rand -hex 12)"

cp .env.example .env
sed -i.bak "s|^DB_PASSWORD=.*|DB_PASSWORD=${DB_PASSWORD}|"       .env
sed -i.bak "s|^JWT_SECRET=.*|JWT_SECRET=${JWT_SECRET}|"          .env
sed -i.bak "s|^ADMIN_EMAIL=.*|ADMIN_EMAIL=admin@example.com|"    .env
sed -i.bak "s|^ADMIN_PASSWORD=.*|ADMIN_PASSWORD=${ADMIN_PASSWORD}|" .env
rm -f .env.bak

echo "Created .env with generated secrets."
echo "Bootstrap admin: admin@example.com  (password is in .env -> ADMIN_PASSWORD)"
