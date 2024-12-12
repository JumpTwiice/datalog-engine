profiler="profiles/$1-profile.json"
resdir="result/$1"
outputdir="output/$1"
mkdir "profiles" -p
mkdir "$resdir" -p
mkdir "$outputdir" -p
for i in $(seq 1 "$2");
do
  souffle -F. -D./"$outputdir" "$1.dl" -p "$profiler" && souffleprof "$profiler" -c rel > "$resdir/$i.txt"
done