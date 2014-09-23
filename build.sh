# Must have $HADOOP_HOME and $HADOOP_CORE set, ie:
# HADOOP_HOME='/opt/hadoop'
HADOOP_CORE='hadoop-core-0.20.203.0.jar'
# OR
# HADOOP_CORE='hadoop-0.20.1-core.jar'

rm -rf ff_classes
mkdir ff_classes
javac -classpath ${HADOOP_HOME}/$HADOOP_CORE -d ff_classes/ FlexiFaCT.java FFMapper.java FFReducer.java Tensor.java SparseTensor.java DenseTensor.java Matrix.java FFMapperPaired.java FFPartitioner.java KeyComparator.java GroupComparator.java IntArray.java FloatArray.java
jar -cvf FlexiFaCT.jar -C ff_classes/ .
rm -rf ff_classes


rm -rf loss_classes
mkdir loss_classes
javac -classpath ${HADOOP_HOME}/$HADOOP_CORE -d loss_classes/ Loss.java FFMapper.java Tensor.java SparseTensor.java DenseTensor.java Matrix.java FFMapperPaired.java FFPartitioner.java KeyComparator.java GroupComparator.java IntArray.java FloatArray.java
jar -cvf Loss.jar -C loss_classes/ .
rm -rf loss_classes

rm FloatReader.class
javac -g FloatReader.java

rm SaveColumns.class
javac -g SaveColumns.java
