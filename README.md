## Introduction

This repo implements a plugin for Mindustry server. The plugin comes with the following server commands:  
- Teleporting
  - `tp`
    - `tp <x> <y>`: Teleport to a location
    - `tp <player>`: Teleport yourself to a player
  - `here`: Teleport yourself to the location your mouse is pointing at
  - `back`: Teleport yourself back to the location you were at before teleporting, death or respawn
- Pausing and resuming the game
  - `pause <on|off>`: Pause or resume the game
  - `p`: Toggle paused state of the game

## Notes from official template  
The following is the original README.md from the official plugin template which should give you an insight into how to
get the plugin up and running. 

### Setup

Clone this repository first.
To edit the plugin display name and other data, take a look at `plugin.json`.
Edit the name of the project itself by going into `settings.gradle`.

### Basic Usage

See `src/example/ExamplePlugin.java` for some basic commands and event handlers.  
Every main plugin class must extend `Plugin`. Make sure that `plugin.json` points to the correct main plugin class.

Please note that the plugin system is in beta, and as such is subject to changes.

### Building a Jar

`gradlew jar` / `./gradlew jar`

Output jar should be in `build/libs`.


### Installing

Simply place the output jar from the step above in your server's `config/mods` directory and restart the server.
List your currently installed plugins/mods by running the `mods` command.
