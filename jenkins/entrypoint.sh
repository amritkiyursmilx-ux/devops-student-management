#!/bin/sh
# ---------------------------------------------------------------------------
# Jenkins container entrypoint.
#
# Docker Desktop recreates /var/run/docker.sock as root:root whenever the
# WSL2 engine restarts. The 'docker' group created by this image then has no
# access to the socket, and every pipeline run fails at the "Verify
# Environment" stage with:
#
#   permission denied while trying to connect to the Docker daemon socket
#   at unix:///var/run/docker.sock
#
# This entrypoint restores the group ownership before Jenkins starts, then
# drops back to the unprivileged 'jenkins' user so the server never runs as
# root.
# ---------------------------------------------------------------------------
set -e

SOCKET=/var/run/docker.sock

# The container image inherits HOME=/root from the base image. The server runs
# as the unprivileged 'jenkins' user, so HOME must be pointed at that user's
# own home directory. Otherwise the Docker CLI looks for its configuration and
# the buildx plugin under /root/.docker, which the jenkins user cannot read,
# and "docker build" fails with "unknown shorthand flag: 't' in -t".
export HOME=/var/jenkins_home

# The Docker CLI stores its configuration and the buildx plugin state under
# $HOME/.docker. Anything left there by a root process is unreadable for the
# jenkins user, which breaks "docker build" with a permission error. Make
# sure the whole directory belongs to the user the server runs as.
if [ -d "$HOME/.docker" ]; then
    chown -R jenkins:jenkins "$HOME/.docker" 2>/dev/null || true
fi

if [ -S "$SOCKET" ]; then
    docker_gid=$(getent group docker | cut -d: -f3)
    socket_gid=$(stat -c '%g' "$SOCKET")

    if [ -n "$docker_gid" ] && [ "$docker_gid" != "$socket_gid" ]; then
        echo "Restoring Docker socket group: gid $socket_gid -> $docker_gid"
        chown ":$docker_gid" "$SOCKET" || echo "Warning: could not change the Docker socket group."
    fi
fi

# Run the server as the unprivileged 'jenkins' user. tini stays as PID 1 so
# that signals are forwarded and zombie processes are reaped.
exec setpriv --reuid=jenkins --regid=jenkins --init-groups \
    /usr/bin/tini -- /usr/local/bin/jenkins.sh "$@"
