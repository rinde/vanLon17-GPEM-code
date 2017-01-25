import os

for file in os.listdir("files/heuristics/"):
	if file.endswith(".dot"):
		print(file)
		cmd = "dot files/heuristics/"+file+" -Tpng > files/heuristics/" + file.replace(".dot",".png")
		os.system(cmd)
