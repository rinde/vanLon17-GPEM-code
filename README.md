# Optimizing agents with genetic programming - An evaluation of hyper-heuristics in dynamic real-time logistics

This repository contains the code that was used to perform the experiments described in:

 > *Optimizing agents with genetic programming - An evaluation of hyper-heuristics in dynamic real-time logistics.* Rinde R.S. van Lon, Juergen Branke, and Tom Holvoet. Genetic Programming and Evolvable Machines (2017).

This repository is archived on Zenodo: [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.260130.svg)](https://doi.org/10.5281/zenodo.260130)

The datasets and results belonging to this paper can be found at this location: 
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.259774.svg)](https://doi.org/10.5281/zenodo.259774)

## Overview

Since the paper is part of a long term research effort, the code used for the experiments is distributed over several open source repositories. The code in the current repository is the glue that instantiates and binds the code from the other repositories to create a cohesive experiment setup.

### In this repository

This repository contains several scripts that can be used to execute each experiment conducted for this paper. Maven and Java 7 (or higher) are required. Beware that some of the scripts below require extensive computational resources, see the paper for more details.

| Purpose of script        							| Prerequisites 																		| Command 
| ------------- 									| -------------																			| -------------
| Generate train dataset. Generates all 90.000 train scenarios in ```files/dataset10k/```, this took about 36 minutes on a 24 core machine (fully utilizing all cores). The Java code can be found [here](src/main/java/com/github/rinde/gpem17/GenerateTrainDataset.java).	The train dataset is part of the dataset and results archive, downloadable from the link above. | The dataset requires  about 4.9 GB of disk space.	| ```./generate-train-dataset.sh```
| Generate parameter files for evolution and tuning experiments. The files are written to ```files/config/experiments/``` and ```files/config/tuning-experiments/```. | 																			| ```./generate-params.sh```
| Perform evolution experiment. By default it starts a distributed experiment using the [JPPF framework](http://jppf.org/). For this to work, a JPPF server (version 4.1.3) needs to be running at localhost. If you want to run the experiment locally, you can change the parameter ```eval.distributed``` in ```files/config/gpem17common.params``` to ```false``` and generate new parameter files. | Requires train dataset in ```files/dataset10k/```. Requires  parameter files in ```files/config/experiments/```. 																		| ```./train-main.sh```
| Perform evaluation experiment. 					| The test dataset in ```files/vanLonHolvoet15/```, can be downloaded from [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.209760.svg)](https://doi.org/10.5281/zenodo.209760).															| ```./evaluate-main.sh```
| Time measuring experiment. 						| The test dataset in ```files/vanLonHolvoet15/```, can be downloaded from [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.209760.svg)](https://doi.org/10.5281/zenodo.209760).																						| ```./time-measure-exp.sh```
| Visualize heuristics, creates .png files for each heuristic in ```files/epxeriment-overview.csv``` and writes them to ```files/heuristics/```. 								| [Python](https://www.python.org/) (the script was developed using version 2.7.13rc1), ```dot``` part of [Graphviz](http://graphviz.org/) (version 2.38.0)						| ```./visualize-heuristics.sh```
| Tuning experiment. 								| Requires train dataset in ```files/dataset10k/```. Requires  parameter files in ```files/config/tuning-experiments/```.		| ```./train-tuning.sh```

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
