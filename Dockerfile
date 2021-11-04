FROM openjdk:11

#Le instalamos curl. Que viene bien para ver conectividad.
#RUN apk add --update \
#    curl \
#    && rm -rf /var/cache/apk/*

#VOLUME /tmp

#Opcion funciona con linea de comandos
#COPY build/libs/txMemPool-0.0.1-SNAPSHOT.jar app.jar
#ENTRYPOINT ["java", "-jar", "app.jar"]

#Opcion funciona con ejemplo de spring boot
#ARG DEPENDENCY=target/dependency
#COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
#COPY ${DEPENDENCY}/META-INF /app/META-INF
#COPY ${DEPENDENCY}/BOOT-INF/classes /app
#ENTRYPOINT ["java","-cp","app:app/lib/*","com.mempoolexplorer.txmempool.TxMemPoolApplication"]

#Opcion funciona mio
COPY ${DEPENDENCY} /
ENTRYPOINT ["java","-Xmx1500m","-Xms1500m","-jar","/app/txMemPool-0.0.1-SNAPSHOT.jar"]
