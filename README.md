todo


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
