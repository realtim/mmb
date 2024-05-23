FROM mariadb:10.4

RUN apt-get update \
    && apt install -y pv \
    && apt-get autoremove -y \
    && apt-get autoclean \
    && rm -r /var/lib/apt/lists/*

COPY Fixture.sql /tmp/Fixture.sql