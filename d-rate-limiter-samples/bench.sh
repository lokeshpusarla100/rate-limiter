#!/bin/bash
RESULTS_FILE="results.txt"
> $RESULTS_FILE
for i in {1..150}; do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/limited >> $RESULTS_FILE &
done
wait
sort $RESULTS_FILE | uniq -c
rm $RESULTS_FILE
