# comp215-api-tool
A little tool that allows a simple filter layer over the github events
api.

```console
usage: apitool [-h] -r REQUEST [-f FILTER] [-d DEADLINE]

required arguments:
  -r REQUEST,           specify the GitHub api request URL to use
  --request REQUEST


optional arguments:
  -h, --help            show this help message and exit

  -f FILTER,            filter the API results by a manual JsonPath
  --filter FILTER       query

  -d DEADLINE,          set the deadline for the events, and
  --deadline DEADLINE   highlight late events in red
```

A sample command usage for looking for late submissions could be the following:

```
apitool -r /orgs/RiceComp215/events \
    --filter [?(@.repo.name ~= 'comp215-fall2017-week03-queues-.+')] \
    --deadline 2017-09-13T24:00:00Z
```

this would print URLs to access github api JSON information for each "offending repository".

