# TextDiff



## Components
```

./benchmarker           # Used to run all 12,227 tests, with the "How to run" commands.


LeePincombeWelsh/
  complete.csv          # The data for the 12,227 tests
  short.csv             # The data for the short test of 6 text pairs.
  ... 50 text files     # All text files are located here as well...

Samples/                # The samples folder includes different text files which can be used to test the Indexer.

SSR/


```

## How To Run:

````
# Run all tests

./benchmarker


# Index words (add to context relations) in several files:

./indexer <FilePath>   


# Remove weak associations [PURGE]

./indexer -p


# Clean all previous context relations.

rm index.dat

````