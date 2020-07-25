# Postprocessing

## Summary

After each individual style file is passed through the transformer stack and
the output file(s) are generated, each output file will be passed through
a postprocessor stack before being written to disk.

## Postprocessor

A postprocessor will conform to the same interface as a transformer (since
it is basically serving the same function): A module that exposes two functions,
`enter` and `exit`. Each one of these is a function that takes three arguments:

1. A Javascript object with `absolutePath` and `content` keys, representing
   the file to be transformed
1. A Javascript object with any keys, representing the options passed to the
   transformer. The values from `css-gardener.edn` will be populated here when
   the postprocessor is called.
1. An error-first callback, to be called with two arguments: The error (or null),
   and the result. The result should again be a JS object with `absolutePath` and
   `content` keys, as it will be passed in as the first argument of the next
   postprocessor.

The postprocessor stack is specified in the config file like so:

```clj
{:builds
 {...}

 :rules
 {...}

 :postprocessing
 {:transformers [{:node-module "@css-gardener/postcss-transformer"}]}}
```
