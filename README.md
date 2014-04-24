FlexiFaCT
====

FlexiFaCT is a system for performing scalable matrix, tensor, and coupled
matrix-tensor factorization on top of standard, stock Hadoop.  The system is
flexible to different objectives, including non-negative factorization and
sparse factorization.  Through careful partitioning of both the data and the
model, our system can scale to big data and models with even billions of
parameters.

For details of the algorithm and implementation (such as how to set parameters)
see [1], and if you are interested in this direction of research you can check
out our follow up work in [2]. 


Running
====

The implementation of FlexiFaCT has been tested on Hadoop 0.20.1 and Java (but
can likely be run on other versions of Hadoop).

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
change the appropriate directories.  

Test data can be generated with `makebig.py`.

Authors
=======

FlexiFaCT is developed by:

Alex Beutel, Carnegie Mellon University - http://alexbeutel.com/

Abhimanu Kumar, Carnegie Mellon University  - http://www.abhimanukumar.com/

Evangelos Papalexakis, Carnegie Mellon University  - http://www.cs.cmu.edu/~epapalex/

Partha Pratim Talukdar, Carnegie Mellon University  - http://talukdar.net/

Christos Faloutsos, Carnegie Mellon University  - http://www.cs.cmu.edu/~christos/

Eric P. Xing, Carnegie Mellon University  - http://www.cs.cmu.edu/~epxing/


References
====

[1] Alex Beutel, Abhimanu Kumar, Evangelos Papalexakis, Partha Pratim Talukdar,
Christos Faloutsos, Eric P. Xing.  "FlexiFaCT: Scalable Flexible Factorization
of Coupled Tensors on Hadoop."  2014 SIAM International Conference on Data
Mining (SDM).  
[PDF](http://alexbeutel.com/papers/sdm2014.flexifact.pdf)

[2] Abhimanu Kumar, Alex Beutel, Qirong Ho, Eric P. Xing.
"Fugue: Slow-Worker-Agnostic Distributed Learning for Big Models."
17th International Conference on Artificial Intelligence and Statistics (AISTATS), 2014. 
[PDF](http://alexbeutel.com/papers/aistats2014.fugue.pdf)
