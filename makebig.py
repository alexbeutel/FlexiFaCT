# Copyright 2016 Alex Beutel alex@beu.tel                                  
#                                                                          
# Licensed under the Apache License, Version 2.0 (the "License");          
# you may not use this file except in compliance with the License.         
# You may obtain a copy of the License at                                  
#                                                                          
# 	http://www.apache.org/licenses/LICENSE-2.0                           
#                                                                          
# Unless required by applicable law or agreed to in writing, software      
# distributed under the License is distributed on an "AS IS" BASIS,        
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
# See the License for the specific language governing permissions and      
# limitations under the License.                                           

import sys
import numpy as np
import math
import shlex
import random

def main():

	nonzeros = 10000000
	nonzeros2 = 10000000
	D = 10000
	rank = 30

	print "Generate Parameters"
	U = 0.5 * np.random.random_sample((D,rank))
	V = 0.5 * np.random.random_sample((D,rank))
	W = 0.5 * np.random.random_sample((D,rank))
	A = 0.5 * np.random.random_sample((D,rank))

	outdata = 'tensor.txt'
	outdata2= 'matrix.txt'

	used = set()
	
	print "Generate Tensor"
	output = open(outdata, 'w')
	cnt = 0
	while True:
		i = random.randint(0,D-1)
		j = random.randint(0,D-1)
		k = random.randint(0,D-1)
		st = str(i) + "," + str(j) + "," + str(k)
		if st not in used:
			cnt = cnt + 1
			used.add(st)
			val = np.sum(U[i] * V[j] * W[k])
			output.write(str(i) + '\t' + str(j) + '\t' + str(k) + '\t' + str(val) + '\n')
			if cnt > nonzeros:
				break

	output.close()


	print "Generate Matrix"
	output = open(outdata2, 'w')
	cnt = 0
	used = set()
	while True:
		i = random.randint(0,D-1)
		j = random.randint(0,D-1)
		st = str(i) + "," + str(j)
		if st not in used:
			cnt = cnt + 1
			used.add(st)
			val = np.sum(U[i] * A[j])
			output.write(str(i) + '\t' + str(j) + '\t' + str(val) + '\n')
			if cnt > nonzeros2:
				break

	output.close()




if __name__ == '__main__':
	main()

