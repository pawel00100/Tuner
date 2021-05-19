
## Endpoints

Short description of all REST API endpoints user for development.
| resource                                                           | type | description                                                          | object structure                              |
|--------------------------------------------------------------------|------|----------------------------------------------------------------------|-----------------------------------------------|
| `/api/check`                                                       | get  | adds user                                                            |                                               |
| `/api/recorder/record/channel/{channel}`                           | post | starts recording now (by default for 3 hours)                        |                                               |
| `/api/recorder/record/channel/{channel}/seconds/{time}`            | post | starst recording now for given time in seconds                       |                                               |
| `/api/recorder/record/channel/{channel}/after/{after}/time/{time}` | post | schedules recording for given time after period (both in seconds)    |                                               |
| `/api/recorder/record/channel/{channel}/start/{start}/end/{end}`   | post | schedules recording from epoch second timestamp to another timestamp |                                               |
| `/api/recorder/stop`                                               | post | forces stop of current recording                                     |                                               |
| `/api/channel/list`                                                | get  | returns available channels                                           | [Channel list](/misc/exampleChannelList.json) |
| `/api/epg`                                                         | get  | returns epg                                                          | [EPG list](/misc/exampleEPGlList.json)        |
