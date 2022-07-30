Using the Flexible I/O Tester

# Install FIO
brew install fio

https://linux.die.net/man/1/fio
https://www.binarylane.com.au/support/solutions/articles/1000055889-how-to-benchmark-disk-i-o

mkdir tmp;
cd tmp;

fio 
  # File name and location 
  --filename=/tmp/testfio \
  # Name of the test
  --name=fiotest \
  # Size of the test
  --size=16G \
  # Read/Write distribution
  --readwrite=randrw \
  --rwmixread=75 \
  # Block size (check https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/volume_constraints.html#block_size) 
  --bs=4k \
  
  --iodepth=1 \
  # Direct I/O to device (non-buffered)
  --direct=1 \
  # Reduce gettimeofday() calls
  --gtod_reduce=1 \

fio \
  --filename=./testfio \
  --name=fiotest \
  --size=16G \
  --readwrite=randrw \
  --rwmixread=75 \
  --bs=4k \
  --direct=1 \
  --gtod_reduce=1
