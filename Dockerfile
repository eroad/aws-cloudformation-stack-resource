FROM alpine:3.7
RUN apk --no-cache add python3 ca-certificates jq bc bash && \
    pip3 install awscli --no-cache-dir
ADD bin /opt/resource
