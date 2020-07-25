# Postprocessing

## Summary

After each individual style file is passed through the transformer stack and
the output file(s) are generated, each output file will be passed through
a postprocessor stack before being written to disk.

By default, the postprocessing step should only be done in release mode, as it
may contain expensive operations like minification.

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

## Turning transformers on and off for development and release

Metadata may be added to transformer specs to turn individual transformers on
or off for development and release modes. A transformer will be used in
development mode if the `:dev` metadata key is true, and will be used in
release mode if the `:release` metadata key is true.

For example, to indicate that a transformer should be enabled in development
and not in release mode:

```clj
{:builds
 {...}

 :rules
 {...}

 :postprocessing
 {:transformers [^{:dev true :release false}
                 {:node-module "@css-gardener/postcss-transformer"}]}}
```

By default, transformers under `:rules` will be enabled in `:dev` and `:release`,
and transformers under `:postprocessing` will be enabled in `:release` only.
