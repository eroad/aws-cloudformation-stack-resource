FROM alpine:3.7
RUN apk --no-cache add py-pip curl ca-certificates jq bc bash && \
    pip install awscli
ADD bin /opt/resource
