FROM tomcat:9-jre8-alpine

ENV FCREPO_VERSION=4.8.0-SNAPSHOT
ENV FCREPO_WEBAPP=fcrepo-webapp-${FCREPO_VERSION}.war

WORKDIR /etc/fcrepo
COPY dist/fcrepo-config.xml dist/logback.xml dist/repository.json /etc/fcrepo/
COPY dist/server.xml dist/ca* ${CATALINA_HOME}/conf/

# Fcrepo webapp
COPY dist/${FCREPO_WEBAPP} ${CATALINA_HOME}/webapps/fcrepo.war
RUN echo "org.apache.catalina.webresources.Cache.level = SEVERE" \
    >> ${CATALINA_HOME}/conf/logging.properties



