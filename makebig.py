#!/usr/bin/env python
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

