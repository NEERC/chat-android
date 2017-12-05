#!/bin/sh

SRC=app/build/outputs/apk/neerc/release/app-neerc-release.apk

USER=volunteers
HOST=neerc.ifmo.ru
DEST=volunteers/chat/app-neerc-release.apk

TMP=$DEST.tmp

echo "Uploading artifacts..."
scp $SRC $USER@$HOST:$TMP
ssh $USER@$HOST mv $TMP $DEST
