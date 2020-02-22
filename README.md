# hax

An example application that implements the server-side of the
front-end application `hix`.

Mostly for me to learn re-frame and integrant.

## Installation

Download from https://github.com/syntereen/hax.

## Usage

Run the project directly:

    $ clj -A:prod --main syntereen.hax

Run the project's tests (they'll fail until you edit them):

    $ clj -A:test:runner

Build an uberjar:

    $ clj -A:uberjar

Run that uberjar:

    $ java -jar hax.jar

## Options

The `hax` server does not take any options. Other than the alias `dev`
for development and `prod` for production.

All other options are provided in the config file `system.edn`.

## Examples

To run `hax` in a `dev` environment,

	$ emacs

Then visit the `deps.edn` (or any other Clojure file).

Then fire up cider, with `Ctl-C M-J`.

Once you have a cider nrepl buffer in the `user` namespace,
run the server using `(go)`.

You can use `(halt)` to stop the system, and `(reset)` to stop,
reload, and restart the system.

## License

Copyright © 2020 Dorab Patel

Distributed under the MIT License.
