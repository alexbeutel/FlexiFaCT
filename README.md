FlexiFaCT
====

Implementation of FlexiFaCT generally uses Hadoop 0.20.1 and Java.

Include in your bashrc:

```bash
export PATH=$PATH:/hadoop/hadoop-current/bin
export PATH=$PATH:/hadoop/jdk-current/bin

alias hls='hadoop fs -ls '
alias hput='hadoop fs -put '
alias hmv='hadoop fs -mv '
alias hmkdir='hadoop fs -mkdir '
alias hcat='hadoop fs -cat '

```

You must also have the `HADOOP_HOME` and `HADOOP_CORE` set to compile the
necessary jar files in the `build.sh` script.

An example of running the script can be found in `run.sh` but make sure to
change the directories.  

Test data can be generated with `makebig.py`.
