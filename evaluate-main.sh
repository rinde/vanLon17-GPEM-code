mvn clean compile exec:java -Dexec.mainClass="com.github.rinde.gpem17.eval.Evaluate" \
-Dexec.args="\
realtime \
all-heuristics.txt \
EVO \
1-1-1 \
OptaPlanner \
DisableTimeMeasurements \
heuristic-comp-delay:0ms \
run-optaplanner-mas:false \
-repetitions 1" &