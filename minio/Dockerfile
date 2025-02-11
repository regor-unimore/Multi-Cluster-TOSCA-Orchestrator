FROM minio/minio

ENV MINIO_ROOT_USER test
ENV MINIO_ROOT_PASSWORD testtest

COPY minio-init.sh /usr/local/bin/minio-init.sh

RUN chmod +x /usr/local/bin/minio-init.sh

ENTRYPOINT ["/usr/local/bin/minio-init.sh"]