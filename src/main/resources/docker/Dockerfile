FROM pandorasystems/karaf:${KARAF_VERSION}

ENV KARAF_VERSION=${KARAF_VERSION}

COPY repository/org/fcrepo/fcrepo-webapp/${version}/fcrepo-webapp-${version}.jar ${KARAF_DEPLOY}
COPY repository/org/fcrepo/fcrepo-http-commons/${version}/fcrepo-http-commons-${version}.jar ${KARAF_DEPLOY}
COPY repository/org/fcrepo/fcrepo-metrics/${version}/fcrepo-metrics-${version}.jar ${KARAF_DEPLOY}
COPY repository/org/fcrepo/fcrepo-kernel-api/${version}/fcrepo-kernel-api-${version}.jar ${KARAF_DEPLOY}
COPY repository/org/fcrepo/fcrepo-kernel-modeshape/${version}/fcrepo-kernel-modeshape-${version}.jar ${KARAF_DEPLOY}
COPY repository/org/fcrepo/fcrepo-http-api/${version}/fcrepo-http-api-${version}.jar ${KARAF_DEPLOY}
COPY repository/org/fcrepo/fcrepo-event-serialization/${version}/fcrepo-event-serialization-${version}.jar ${KARAF_DEPLOY}
COPY repository/org/fcrepo/fcrepo-jms/${version}/fcrepo-jms-${version}.jar ${KARAF_DEPLOY}
COPY repository/org/fcrepo/modeshape*.jar ${KARAF_DEPLOY}
COPY repository/org/fcrepo/xbean*.jar ${KARAF_DEPLOY}

WORKDIR /opt/karaf/apache-karaf-${KARAF_VERSION}
COPY repository/org/fcrepo/bundles.yml .
RUN bin/start && \
bin/client -r 10 -d 5  "feature:install pax-war" && \
cat bundles.yml | bin/client -b && \
bin/stop

EXPOSE 8181

RUN mkdir /mnt/fcrepo
COPY repository/org/fcrepo/*.cfg etc/
COPY repository/org/fcrepo/entrypoint.sh /entrypoint.sh

RUN chmod 700 /entrypoint.sh
ENTRYPOINT [ "/entrypoint.sh" ]

CMD [ "server" ]