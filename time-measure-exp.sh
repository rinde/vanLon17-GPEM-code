nohup mvn clean compile -U exec:java -Dexec.mainClass="com.github.rinde.gpem17.eval.Evaluate" \
-Dexec.args="\
realtime \
time-measure-heuristic.txt \
EVO \
1-1-1 \
OptaPlanner \
EnableTimeMeasurements \
heuristic-comp-delay:0ms \
run-optaplanner-mas:true \
-repetitions 1 \
-sf regex:.*0\.50-20-10\.00-0\.scen" &