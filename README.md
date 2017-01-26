# Optimizing agents with genetic programming - An evaluation of hyper-heuristics in dynamic real-time logistics

This repository contains the code that was used to perform the experiments described in:

 > *Optimizing agents with genetic programming - An evaluation of hyper-heuristics in dynamic real-time logistics.* Rinde R.S. van Lon, Juergen Branke, and Tom Holvoet. Genetic Programming and Evolvable Machines (2017).


## Overview

Since the paper is part of a long term research effort, the code used for the experiments is distributed over several open source repositories. The code in the current repository is the glue that instantiates and binds the code from the other repositories to create a cohesive experiment setup.

### In this repository

This repository contains several scripts that can be used to execute each experiment conducted for this paper. Maven and Java 7 (or higher) are required.

| Purpose of script        							| Prerequisites 																		| Command 
| ------------- 									| -------------																			| -------------
| Generate train dataset. Generating all 90.000 train scenarios (in dataset10k) took about 36 minutes on a 24 core machine (fully utilizing all cores). The Java code can be found [here](src/main/java/com/github/rinde/gpem17/GenerateTrainDataset.java).							| 																						| ```./generate-train-dataset.sh'''
| generate params 									|																						|
| Perform evolution experiment 						| train dataset 																		|
| Perform evaluation experiment 					| test dataset (LINK)																	|
| time measuring experiment 						| test dataset																						| ```./time-measure-exp.sh'''
| Visualize heuristics, creates .png files for each heuristic in ```files/epxeriment-overview.csv``` and writes them to ```files/heuristics/```. 								| [Python](https://www.python.org/) (the script was developed using version 2.7.13rc1), ```dot``` part of [Graphviz](http://graphviz.org/) (version 2.38.0)						| ```./visualize-heuristics.sh'''
| tuning experiment 								|																						|

### Java dependencies

All Java dependencies are imported via Maven but can also be downloaded manually. The following dependencies are especially relevant:

| Library										| Description																									| Version		| DOI
| -------------									| ------------- 																								| ------------- | -------------
| [RinSim](https://github.com/rinde/RinSim)		| Real-time logistics simulator.																				| [4.3.0](https://github.com/rinde/RinSim/releases/tag/v4.3.0)		    | [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.192106.svg)](https://doi.org/10.5281/zenodo.192106)
| [RinLog](https://github.com/rinde/RinLog)		| Collection of algorithms, including DynCNET multi-agent system and OptaPlanner for dynamic PDPTW problems.	| [3.2.0](https://github.com/rinde/RinLog/releases/tag/v3.2.0)         | [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.192111.svg)](https://doi.org/10.5281/zenodo.192111)
| [PDPTW Dataset Generator](https://github.com/rinde/pdptw-dataset-generator)	| Generator of dynamic PDPTW datasets.													| [1.1.0](https://github.com/rinde/pdptw-dataset-generator/releases/tag/v1.1.0)			| [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.59259.svg)](https://doi.org/10.5281/zenodo.59259)
| [RinECJ](https://github.com/rinde/RinECJ) | Wrapper for [ECJ](http://cs.gmu.edu/~eclab/projects/ecj/) that simplifies configuration of genetic programming. |[0.3.0](https://github.com/rinde/RinECJ/releases/tag/v0.3.0) | [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.259718.svg)](https://doi.org/10.5281/zenodo.259718)
| [evo4mas](https://github.com/rinde/evo4mas) | Provides support for evolved hyper-heuristics that work with RinLog abstractions. | [0.3.0](https://github.com/rinde/evo4mas/releases/tag/v0.3.0) | [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.248966.svg)](https://doi.org/10.5281/zenodo.248966) 



### License

All files in this repository are licensed under the [Apache 2.0 license](LICENSE).
