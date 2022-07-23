#/bin/bash

MAJOR=1
MINOR=0
PATCH=$(date +%Y%m%d%H%M%S)
VERSION="$MAJOR.$MINOR.$PATCH"
TAG="v$VERSION"
NOTES=${NOTES:-"AWS pod release $VERSION"}
git tag $TAG -m "$NOTES"
git push origin $TAG
