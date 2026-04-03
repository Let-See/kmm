#!/usr/bin/env bash
set -euo pipefail

# ── Central Portal native Publisher API upload ──────────────────────────
# Builds all artifacts locally, bundles them into a zip, and uploads
# to https://central.sonatype.com via the REST API.
#
# Required env vars (or source .env first):
#   CENTRAL_PORTAL_USERNAME  – user-token username from Central Portal
#   CENTRAL_PORTAL_PASSWORD  – user-token password from Central Portal
#   SIGNING_KEY_ID           – GPG key ID
#   SIGNING_PASSWORD         – GPG passphrase
#   SIGNING_KEY              – ASCII-armored private key (or set via: export SIGNING_KEY="$(cat private-key.asc)")
#
# Usage:
#   source .env && ./publish-to-central.sh
#   # or with auto-publish:
#   source .env && ./publish-to-central.sh --auto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

STAGING_DIR="build/staging-deploy"
BUNDLE_FILE="build/central-bundle.zip"
PUBLISH_TYPE="USER_MANAGED"

if [[ "${1:-}" == "--auto" ]]; then
  PUBLISH_TYPE="AUTOMATIC"
  echo "Mode: AUTOMATIC (will publish without manual approval)"
else
  echo "Mode: USER_MANAGED (you'll click Publish in the portal)"
  echo "  Tip: pass --auto to publish automatically after validation."
fi

# ── Validate credentials ────────────────────────────────────────────────
for var in CENTRAL_PORTAL_USERNAME CENTRAL_PORTAL_PASSWORD; do
  if [[ -z "${!var:-}" ]]; then
    echo "ERROR: $var is not set. Source your .env first."
    exit 1
  fi
done

# ── Step 1: Build & sign artifacts into local staging dir ───────────────
echo ""
echo "==> Step 1/4: Building and signing artifacts..."
rm -rf "$STAGING_DIR"
./gradlew --no-daemon publishAllPublicationsToStagingRepository

if [[ ! -d "$STAGING_DIR" ]]; then
  echo "ERROR: Staging directory $STAGING_DIR was not created."
  exit 1
fi

# ── Step 2: Create zip bundle ───────────────────────────────────────────
echo ""
echo "==> Step 2/4: Creating bundle zip..."
rm -f "$BUNDLE_FILE"
(cd "$STAGING_DIR" && zip -r "../../$BUNDLE_FILE" .)
echo "  Bundle: $BUNDLE_FILE ($(du -h "$BUNDLE_FILE" | cut -f1))"

# ── Step 3: Upload to Central Portal ───────────────────────────────────
echo ""
echo "==> Step 3/4: Uploading bundle to Central Portal..."
AUTH_TOKEN="$(printf '%s:%s' "$CENTRAL_PORTAL_USERNAME" "$CENTRAL_PORTAL_PASSWORD" | base64)"

RESPONSE_FILE="$(mktemp)"
HTTP_CODE=$(curl --silent --show-error --output "$RESPONSE_FILE" --write-out "%{http_code}" \
  --request POST \
  --header "Authorization: Bearer ${AUTH_TOKEN}" \
  --form "bundle=@${BUNDLE_FILE}" \
  "https://central.sonatype.com/api/v1/publisher/upload?publishingType=${PUBLISH_TYPE}")

HTTP_BODY="$(cat "$RESPONSE_FILE")"
rm -f "$RESPONSE_FILE"

if [[ "$HTTP_CODE" -ge 200 && "$HTTP_CODE" -lt 300 ]]; then
  DEPLOYMENT_ID="$HTTP_BODY"
  echo "  Upload successful! Deployment ID: $DEPLOYMENT_ID"
else
  echo "  ERROR: Upload failed (HTTP $HTTP_CODE)"
  echo "  Response: $HTTP_BODY"
  exit 1
fi

# ── Step 4: Poll deployment status ──────────────────────────────────────
echo ""
echo "==> Step 4/4: Checking deployment status..."
echo "  Deployment: https://central.sonatype.com/publishing/deployments"
echo ""

for i in $(seq 1 30); do
  sleep 10
  STATUS_RESPONSE=$(curl --silent --show-error \
    --request POST \
    --header "Authorization: Bearer ${AUTH_TOKEN}" \
    --header "Content-Type: application/json" \
    --data "{\"id\":\"$DEPLOYMENT_ID\",\"name\":\"\",\"deploymentState\":\"\"}" \
    "https://central.sonatype.com/api/v1/publisher/status?id=${DEPLOYMENT_ID}" 2>/dev/null || true)

  DEPLOY_STATE=$(echo "$STATUS_RESPONSE" | grep -o '"deploymentState":"[^"]*"' | head -1 | cut -d'"' -f4 || echo "UNKNOWN")

  echo "  [$i/30] Status: $DEPLOY_STATE"

  case "$DEPLOY_STATE" in
    PUBLISHED)
      echo ""
      echo "Published successfully to Maven Central!"
      exit 0
      ;;
    VALIDATED)
      if [[ "$PUBLISH_TYPE" == "USER_MANAGED" ]]; then
        echo ""
        echo "Validation passed! Go to https://central.sonatype.com/publishing/deployments"
        echo "and click 'Publish' on deployment $DEPLOYMENT_ID to release."
        exit 0
      fi
      ;;
    FAILED)
      echo ""
      echo "Deployment FAILED. Check https://central.sonatype.com/publishing/deployments"
      echo "for details on deployment $DEPLOYMENT_ID."
      exit 1
      ;;
  esac
done

echo ""
echo "Timed out waiting for deployment. Check manually:"
echo "  https://central.sonatype.com/publishing/deployments"
