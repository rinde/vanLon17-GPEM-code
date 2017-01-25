# Optimizing agents with genetic programming - An evaluation of hyper-heuristics in dynamic real-time logistics

This repository contains the code that was used to perform the experiments described in:

 > *Optimizing agents with genetic programming - An evaluation of hyper-heuristics in dynamic real-time logistics.* Rinde R.S. van Lon, Juergen Branke, and Tom Holvoet. Genetic Programming and Evolvable Machines (2017).


## Overview

Since the paper is part of a long term research effort, the code used for the experiments is distributed over several open source repositories. The code in the current repository is the glue that instantiates and binds the code from the other repositories to create a cohesive experiment setup.

### In this repository

This repository contains several scripts that can be used to execute each experiment conducted for this paper. Maven and Java 7 (or higher) are required.

| Purpose of script        							| Prerequisites 																		| Command 
| ------------- 									| -------------																			| -------------
| Generate train dataset 							| 																						| ```./generate-train-dataset.sh'''
| generate params 									|																						|
| Perform evolution experiment 						| train dataset 																		|
| Perform evaluation experiment 					| test dataset (LINK)																	|
| time measuring experiment 						|																						| ```./time-measure-exp.sh'''
| Visualize heuristics 								| python (the script was developed using Python 2.7.13rc1) / dot						|
| tuning experiment 								|																						|

## Code for generating train dataset

The code for generating the train dataset can be found in [GenerateTrainDataset](src/main/java/com/github/rinde/gpem17/GenerateTrainDataset.java).

To generate the train dataset, one can execute (in the project folder):

```shell
mvn clean compile exec:java -Dexec.mainClass="com.github.rinde.gpem17.GenerateTrainDataset"
```

Generating all 13.500 train scenarios takes about 1 hour and 45 minutes on a 24 core machine (fully utilizing all cores). One can also use the generated files as described in the next section.


Generating all 45.000 train scenarios (in dataset5000) took about 18 minutes on a 24 core machine (fully utilizing all cores).

Generating all 90.000 train scenarios (in dataset10k) took about 36 minutes on a 24 core machine (fully utilizing all cores).


```shell
./time-measure-exp.sh
```
