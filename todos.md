# TODOs

## Engine
- ~~Generify BackgroundWorker even more (custom intermediate type instead of CopyProgress)~~
- add logging to engine
  - create a session log
  - implement the possibility to print session logs to file on disc
  - include problem reports
- enhance exception handling
  - top level exception in engine should be context-related exception
  - top level exception in gui should be "copysnap"-related exception
  - maybe add exeptions to logging

## Gui
- add settings to gui
  - persist settings to database, to load contexts that were lately in use
- ~~initial focus on text bars as soon as they appear~~
- ~~text completion on text bars~~
- intuitive focus in gui + shortcuts for navigating focus to components
  - Key-Listener for Arrow keys on path selection bars?

## Cli
- create cli