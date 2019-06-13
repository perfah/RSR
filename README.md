# SSR
A testing environment for Scalable Semantic Relatedness (SSR), a distributional semantics model aiming to quantify how close two texts are conceptually.

## Usage

Calibrate the index:
```console
./SSR/indexer <file_or_dir_to_learn_from> <association_span>
```

List associations in index:
```console
./SSR/indexer --list
```

Remove index to start over:
```console
rm -r SSR/index
```

Purge stop words in index:
```console
./SSR/indexer --purge [--preview]
```

Benchmark SSR:
```console
./benchmarker <LeePinCombeWelsh/complete.sh|LeePinCombeWelsh/short.sh> [--sort]
```

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
