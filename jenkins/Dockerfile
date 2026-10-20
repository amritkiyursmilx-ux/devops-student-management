FROM jenkins/jenkins:lts-jdk21

USER root

# Install Maven, Git, Docker CLI, and Docker Buildx
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        maven \
        docker-cli \
        docker-buildx && \
    rm -rf /var/lib/apt/lists/*

# Add the jenkins user to a docker group so it can reach /var/run/docker.sock.
# Only the docker group is granted - adding jenkins to the root group would give
# it far more privilege than the pipeline needs.
RUN groupadd -f docker && usermod -aG docker jenkins

USER jenkins
