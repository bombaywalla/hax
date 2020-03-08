
# hax

An example application that implements the server-side of the
front-end application `hix`.

Mostly for me to learn re-frame and integrant.

## Installation

Download from https://github.com/syntereen/hax.

## Usage

Run the project directly:

    $ clj -A:prod --main syntereen.hax

or, if using Java9+

	$ clj -A:prod:java9 --main syntereen.hax

Run the project's tests:

    $ clj -A:test:runner

or, if using Java9+

	$ clj -A:test:runner:java9

Build an uberjar:

    $ clj -A:uberjar

Run that uberjar:

    $ java -jar hax.jar

## Options

The `hax` server does not take any options. Other than the alias `dev`
for development, `prod` for production, and `test` for testing.
See also, the `runner` alias for running tests.

All other options are provided in the config file `system.edn`.

## Run in development mode

To run `hax` in a `dev` environment,

	$ emacs

Then visit the `deps.edn` (or any other Clojure file).

Then fire up cider, with `C-c M-J`.

Once you have a cider nrepl buffer in the `user` namespace,
run the server using `(go)`.

You can use `(halt)` to stop the system, and `(reset)` to stop,
reload, and restart the system.

## Run tests

To run `hax` in a `test` environment,

	$ emacs

Then visit the `deps.edn` (or any other Clojure file).

Then fire up cider, with `C-u C-c M-J`.

Edit the command line to replace `dev`with `test`. Then `ENTER`.

Once you have a cider nrepl buffer in the `user` namespace,

	(require 'syntereen.hax-test)
	(in-ns 'syntereen-hax-test)

To start the server,

	(go)

To run all the tests in the `syntereen.hax-test` namespace,

	(ct/run-tests)

You can use `(halt)` to stop the system.

You can also run the tests without `emacs` as shown in the Usage section.

## License

Copyright © 2020 Dorab Patel

Distributed under the MIT License.
