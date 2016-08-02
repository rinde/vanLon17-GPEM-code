todo


## Code for generating train dataset

The code for generating the train dataset can be found in [GenerateTrainDataset](src/main/java/com/github/rinde/sec17/GenerateTrainDataset.java).

To generate the train dataset, one can execute (in the project folder):

```shell
mvn clean compile exec:java -Dexec.mainClass="com.github.rinde.sec17.GenerateTrainDataset"
```

Generating all 13.500 train scenarios takes about 1 hour and 45 minutes on a 24 core machine (fully utilizing all cores). One can also use the generated files as described in the next section.