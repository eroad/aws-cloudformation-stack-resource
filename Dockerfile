FROM busybox:glibc
COPY /alias-scripts/* /opt/resource/
COPY target/cloudformation-resource-*-runner /usr/local/bin/cloudformation-resource
ENTRYPOINT [ "/usr/local/bin/cloudformation-resource" ]