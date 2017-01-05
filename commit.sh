#!/bin/bash
msg="${1:-Default modification}"
git add -A .
git commit -m "$msg"
git push
