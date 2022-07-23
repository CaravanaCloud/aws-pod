FROM gitpod/workspace-full

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh && sdk install java 17.0.3.6.1-amzn && sdk default java 17.0.3.6.1-amzn && sdk install quarkus"
RUN bash -c "curl 'https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip' -o 'awscliv2.zip' && unzip awscliv2.zip && sudo ./aws/install"
RUN bash -c "npm install -g aws-cdk"
RUN bash -c "brew install gh"
RUN bash -c "sudo install-packages mariadb-client"

