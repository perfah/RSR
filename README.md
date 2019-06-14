# SSR
A testing environment for Scalable Semantic Relatedness (SSR), a distributional semantics model aiming to quantify how close two texts are conceptually. SSR uses an incrementally improvable index as a basis for assessing semantic relatedness. This work is a part of our Bachelor's degree project. 

By [Per Fahlander](https://www.github.com/perfah) and [Mattias Bergsström](https://www.github.com/devmattb).

## Usage

**Calibrate the index:**
```console
./SSR/indexer <file_or_dir_to_learn_from> <association_span>
```
- **<file_or_dir_to_learn_from>** : A document file (or a directory containg such) with plain text that can be used for associating words in order to know which go together.
- **<association_span>** : How far two words can be apart and still get associated together.

**List associations in index:**
```console
./SSR/indexer --list
```

**Remove index to start over:**
```console
rm -r SSR/index
```

**Purge (stop) words in index:**
```console
./SSR/indexer --purge [--preview]
```
- **[--preview]** : Shows what words would be removed without action

**Benchmark SSR / evaluate SSR against a set of tests (human opinions):**
Shows the results from specified tests and calculates a Pearson correlations between the algorithmic and human assessments.
```console
./benchmarker <tests_csv_file> [--sort]
```
- **<tests_csv_file>** : the set of tests for the evaluation (e.g. "LeePinCombeWelsh/complete.sh")
- **[--sort]** : The tests SSR performed the worst on will be placed at the end.


## Resources
```

LeePincombeWelsh/
  complete.csv          # The data for the 12,227 tests
  short.csv             # The data for the short test of 6 text pairs.
  ... 50 text files     # All text files are located here as well...

Samples/                # The samples folder includes different text files which can be used to test the Indexer.

SSR/
  indexer.sh
  ...

```
