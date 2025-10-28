#!/bin/sh

FILEPATH="$1"
FILENAME=$(basename "${FILEPATH}")
FILESIZE=$(stat -c%s "${FILEPATH}")
RELEASE_NOTE="$2"

echo "Uploading file: $FILENAME ($FILESIZE bytes)"

# Step 1: Slack 업로드 URL 요청
UPLOAD_RESPONSE=$(curl -s -X POST \
  -H "Authorization: Bearer ${SLACK_TOKEN}" \
  -F "filename=${FILENAME}" \
  -F "length=${FILESIZE}" \
  https://slack.com/api/files.getUploadURLExternal)

UPLOAD_URL=$(echo "$UPLOAD_RESPONSE" | jq -r '.upload_url')
FILE_ID=$(echo "$UPLOAD_RESPONSE" | jq -r '.file_id')

echo "Upload URL: ${UPLOAD_URL}"
echo "File ID: ${FILE_ID}"

# Step 2: 파일 업로드
curl -s -F file=@"${FILEPATH}" "${UPLOAD_URL}"

# Step 3: 업로드 완료 처리
FINAL_BODY=$(jq -n \
  --arg id "$FILE_ID" \
  --arg title "$FILENAME" \
  --arg channel "$SLACK_CHANNEL_ID" \
  --arg comment "$RELEASE_NOTE" \
  '{files: [{id: $id, title: $title}], channel_id: $channel, initial_comment: $comment}')

curl -s -X POST \
  -H "Authorization: Bearer ${SLACK_TOKEN}" \
  -H "Content-Type: application/json; charset=utf-8" \
  -d "$FINAL_BODY" \
  https://slack.com/api/files.completeUploadExternal
