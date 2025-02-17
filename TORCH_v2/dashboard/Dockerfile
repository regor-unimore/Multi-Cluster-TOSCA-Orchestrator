FROM ubuntu:18.04
RUN DEBIAN_FRONTEND=noninteractive \
  apt-get update \
  && apt-get install -y --no-install-recommends tzdata curl \
  #&& apt-get install python3 php7.2 php7.2-mysql php7.2-mbstring -y \
  && apt-get install python3 python3-pip php7.2 php7.2-sqlite3 php7.2-mysql php7.2-mbstring php7.2-xml php7.2-curl -y \
  #&& apt-get install git nodejs npm zip unzip -y \
  && apt-get install git zip unzip -y \
  && rm -rf /var/lib/apt/lists/*

COPY --from=composer:latest /usr/bin/composer /usr/local/bin/composer

ENV NODE_VERSION=14.20.0
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
ENV NVM_DIR=/root/.nvm
RUN . "$NVM_DIR/nvm.sh" && nvm install ${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm use v${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm alias default v${NODE_VERSION}
ENV PATH="/root/.nvm/versions/node/v${NODE_VERSION}/bin/:${PATH}"
RUN node --version
RUN npm --version


COPY dashboard dashboard
COPY dashboard-entrypoint.sh dashboard/entrypoint.sh
RUN chmod +x dashboard/entrypoint.sh

WORKDIR /dashboard
CMD ["./entrypoint.sh"]
