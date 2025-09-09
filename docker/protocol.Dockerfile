#
# 开源代码，仅供学习和交流研究使用，商用请联系三丙
# 微信：mohan_88888
# 抖音：程序员三丙
# 付费课程知识星球：https://t.zsxq.com/aKtXo
#


FROM registry.cn-hangzhou.aliyuncs.com/sanbing/jcpp-base:1.0 AS base
WORKDIR /app
COPY . .
RUN mvn -U -B -T 0.8C clean install -DskipTests

#分层
FROM registry.cn-hangzhou.aliyuncs.com/sanbing/openjdk:21-bullseye AS builder
WORKDIR /app
COPY --from=base /app/jcpp-protocol-bootstrap/target/application.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# 执行
FROM registry.cn-hangzhou.aliyuncs.com/sanbing/openjdk:21-bullseye
RUN useradd -m sanbing
WORKDIR /home/sanbing

COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./
COPY --from=base /app/jcpp-app-bootstrap/target/conf ./config
COPY --from=base /app/docker/start.sh .

RUN chmod a+x start.sh \
    && mkdir -p /home/sanbing/logs/jcpp \
    && mkdir -p /home/sanbing/logs/accesslog \
    && mkdir -p /home/sanbing/logs/gc \
    && mkdir -p /home/sanbing/logs/heapdump \
    && chmod 700 -R /home/sanbing/logs/* \
    && chown -R sanbing:sanbing /home/sanbing

EXPOSE 8080/tcp 38001/tcp 38002/tcp 38003/tcp 38011/tcp

ENV PROTOCOLS_LOG_LEVEL=INFO

USER sanbing
CMD ["/bin/bash", "start.sh"]

