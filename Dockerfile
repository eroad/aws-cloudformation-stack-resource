FROM alpine:3
RUN apk --no-cache add python3 ca-certificates jq bc bash && \
    pip install awscli --no-cache-dir
ADD bin /opt/resource
