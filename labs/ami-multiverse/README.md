# Install SDKMan
```
curl -s "https://get.sdkman.io" | bash
source "/home/ec2-user/.sdkman/bin/sdkman-init.sh"
```

# Install Java
```
sdk list java
sdk install java 22.0.0.2.r17-grl
```

# Install Multiverse Application
```
mkdir demo-multiverse
cd demo-multiverse
URL=https://github.com/CaravanaCloud/aws-pod/releases/download/v1.0.20220723105446/demo-multiverse-1.0.0-SNAPSHOT-runner.jar
OUT=demo-multiverse-1.0.0-SNAPSHOT-runner.jar
curl -Ls $URL --output $OUT
```

Add to/etc/rc.d/rc.local:
```
sudo -u ec2-user bash -c '/home/ec2-user/.sdkman/candidates/java/current/bin/java -jar /home/ec2-user/demo-multiverse/demo-multiverse-1.0.0-SNAPSHOT-runner.jar'
```

Make it executable 
```
chmod +x /etc/rc.d/rc.local
```

Restart the instance:
```
sudo reboot
```

Verify
```
curl -s "http://$(curl -s http://instance-data/latest/meta-data/public-ipv4):8080"
```

Your instance is ready to take an AMI
