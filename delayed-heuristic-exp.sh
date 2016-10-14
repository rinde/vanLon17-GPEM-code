nohup mvn clean compile -U exec:java -Dexec.mainClass="com.github.rinde.gpem17.eval.Evaluate" -Dexec.args="realtime tmp.txt EVO 1-1-1 OptaPlanner EnableTimeMeasurements heuristic-comp-delay:100ms run-optaplanner-mas:false -repetitions 1 -g false -sf regex:.*0\.50-20-10\.00.*\.scen" &

