while true; do
  date '+%Y-%m-%dT%H:%M:%S%z' >> docker-stats
  docker stats --no-stream --format '{{.Name}} {{.MemUsage}}' >> docker-stats
  echo "" >> docker-stats
  sleep 5
done
