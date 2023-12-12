FROM openjdk:7-jdk-alpine3.7 AS BUILD

RUN apk add --no-cache bash

WORKDIR /opt/odinms

COPY /thepack_82 .

WORKDIR /opt/odinms/ThePack

RUN ./compile.sh

FROM openjdk:7-jdk-alpine3.7

COPY --from=ghcr.io/ufoscout/docker-compose-wait:latest /wait /wait

RUN apk add --no-cache bash

COPY --from=BUILD /opt/odinms/ThePack ./odinms

WORKDIR /odinms

EXPOSE 8484 7575 7576 7577 7578
CMD /wait && ./run.sh
