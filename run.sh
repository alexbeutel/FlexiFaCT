#  Copyright 2016 Alex Beutel alex@beu.tel                                  
#                                                                           
#  Licensed under the Apache License, Version 2.0 (the "License");          
#  you may not use this file except in compliance with the License.         
#  You may obtain a copy of the License at                                  
#                                                                           
#  	http://www.apache.org/licenses/LICENSE-2.0                           
#                                                                           
#  Unless required by applicable law or agreed to in writing, software      
#  distributed under the License is distributed on an "AS IS" BASIS,        
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
#  See the License for the specific language governing permissions and      
#  limitations under the License.                                           

dataset_path="/user/abeutel/netflix/"
N=17771
M0=480190
P0=1
key="netflix_test"
lambda=10
mean=0.33
sparse=1
kl=0
nnmf=0
rank=100
d=10
initialStep="0.001" 
min=0.00001
last_iter=$(echo "scale=10; $d*$d-1" | bc -l)

echo -e "Key: $key\nLambda: $lambda\nMean: $mean\nSparse: $sparse\nNNMF: $nnmf\nKL: $kl\nRank: $rank\nd: $d\ninitialStep: $initialStep\nminStep: $min\nLast iteration: $last_iter" > ~/log-$key.txt

hadoop fs -rmr $key/*

params=" -D ff.regularizerLambda=$lambda -D ff.initMean=$mean -D ff.nnmf=$nnmf -D ff.sparse=$sparse -D ff.KL=$kl -D mapred.child.java.opts=-Xmx4096m -D ff.N=$N -D ff.M0=$M0 -D ff.P0=$P0 -D ff.rank=$rank -D mapred.reduce.tasks=$d $d 3 1 $key $dataset_path"
output_dir="/user/abeutel/FFout/$key"


echo "Iteration 0"
step=$initialStep
time hadoop jar FlexiFaCT.jar FlexiFaCT -D ff.stepSize=$step $params ${output_dir}/run0

last=0
for i in {1..15}
do

	step=$(echo "scale=10; $initialStep / (($i + 1) * 0.5)" | bc -l)
	step=$(echo $min $step | awk '{if ($1 < $2) print $2; else print $1}')

	echo "Iteration ${i}"
	echo "Step ${step}"
	time hadoop jar FlexiFaCT.jar FlexiFaCT -D ff.stepSize=$step $params ${output_dir}/run$i ${output_dir}/run${last}/iter${last_iter}

	echo "Loss Iteration ${i}"
	time hadoop jar Loss.jar Loss $params ${output_dir}-loss/run$i ${output_dir}/run${last}/iter${last_iter}

	last=$i
done


#last=0
#for i in {0..14}
#do
	#echo "Loss Iteration ${i}"
	#time hadoop jar Loss.jar Loss $params ${output_dir}-loss/run$i ${output_dir}/run${last}/iter${last_iter}
	#last=$i
#done

