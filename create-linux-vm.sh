#!/bin/bash

# Create Linux VM in Mumbai region
gcloud compute instances create linux-server-mumbai \
  --zone=asia-south1-a \
  --machine-type=e2-medium \
  --image-family=ubuntu-2004-lts \
  --image-project=ubuntu-os-cloud \
  --tags=http-server,https-server \
  --metadata-from-file=startup-script=linux-startup.sh \
  --network-interface=network-tier=PREMIUM,subnet=default \
  --maintenance-policy=MIGRATE \
  --provisioning-model=STANDARD \
  --service-account=YOUR_SERVICE_ACCOUNT@YOUR_PROJECT_ID.iam.gserviceaccount.com \
  --scopes=https://www.googleapis.com/auth/cloud-platform \
  --create-disk=auto-delete=yes,boot=yes,device-name=linux-server-mumbai,image=projects/ubuntu-os-cloud/global/images/family/ubuntu-2004-lts,mode=rw,size=20,type=projects/YOUR_PROJECT_ID/zones/asia-south1-a/diskTypes/pd-standard \
  --no-shielded-secure-boot \
  --shielded-vtpm \
  --shielded-integrity-monitoring \
  --labels=environment=production,app=dashboard \
  --reservation-affinity=any

echo "Linux VM created successfully!"
echo "Getting external IP..."
gcloud compute instances describe linux-server-mumbai --zone=asia-south1-a --format="value(networkInterfaces[0].accessConfigs[0].natIP)" 