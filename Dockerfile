FROM alpine:3
RUN apk --no-cache add py-pip ca-certificates jq bc bash && \
    pip install awscli --no-cache-dir
ADD bin /opt/resource
