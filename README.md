# Mixtape

Command line application that manages song playlists by user. 

## Usage 

_This application runs on MacOS or *nix environment._

Data files used for ingesting can be found in the data directory.

### Prerequisites

**Note: Mixtape application requires a java runtime engine to execute**

To execute the application with sample files:
```
./app/bin/mixtape -i $PWD/data/mixtape-data.json -c $PWD/data/changes-data.csv
```

To view help information for command line arguments:
```
./app/bin/mixtape -h
```

## Scaling the application

The mixtape application ingests the mixtape and changes file using local memory.
The state of the mixtape dataset is also stored in local memory.

As the need grows for the application to scale to support ingesting large input and change files, here is a list of proposed changes:
* Update input and change file processing from loading all in-memory to stream-based chunks
* Update output file processing from all in-memory to stream-based chunks
* (async) Decouple processing of reading files from actual processing using actor model(ie Akka) or distributed streaming framework (ie kafka)
* (async) Decouple processing of input types (users, songs, playlists)
* Leverage a distributed memory store (ie Redis) or NoSQL key/value store (ie AWS DynamomDB) for maintaining state of users, songs, and playlists instead of local memory

## Building, testing, packaging the application

### Prerequisites

For development, you need to install:
 * JDK 1.8
 * scala (preferably 2.13.4)
 * sbt (preferably 1.4.6)

### Build tool commands

Build source:

```sbt clean compile``` 

Run tests:

```sbt clean test```

Package application:

```sbt clean pack```

To execute local build application with sample files:
```
./target/pack/bin/mixtape -i $PWD/data/mixtape-data.json -c $PWD/data/changes-data.csv
```