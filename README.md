ElasticSearch Correctness and perfOrmance Validator
===================================================

This software is in the alpha at this point.

ESCOVA is a ElasticSearch query compiler and verifier. It comprises
two parts: a static analyzer and a performance validator.

ESCOVA Query Language
---------------------

Our query language is essentialy ElasticSearch's, with some additions
to make working with [ESQB][esqb] pleasant, so support for variables is
included in the form of the non-standard construction `{"$var":
{"name": "x", "type": "int" /* , ... */ }}`


Deployment
----------

ESCOVA can either be installed as an Elasticsearch plugin (using the
same data structures as the installed ES instance), or as a standalone
application, which can even be configured to be a frontend proxy to an
Elasticsearch that can be provided as a backend. This allows for
deployment on premises where Elasticsearch cannot be modified, or if
the local system administrator feelds that the plugin security policy
is too broad for their needs (of course, patches are welcome).

### As an Elasticsearch plugin

You will need to build ESCOVA against your specific Elasticsearch
version. Fear not, for we have an ElasticPlugin sbt auto-plugin in the
project that will help you build the ES plugin as easily as with the
Elasticsearch-provided gradle configuration.

You just have to use a line like:

    $ sbt "project esplugin" "esplugin 5.6.3" # For elasticsearch 5.6.3

Keep in mind that source-level breaking changes happen between
Elasticsearch 5 and 6, preventing the same codebase to run on both
Elasticsearch versions. Use `branch_6.x` to build against ES 6.

In the near future we'll migrate and use `branch_5.x` for ES 5 and
push `branch_6.x` to master.

After building, an artifact at
modules/escova-esplugin/target/escova-{version}-for-es-{esversion}.zip
can now be installed via 

    elasticsearch-plugin install file:///path/to/plugin.zip`


### As a standalone Akka application

ESCOVA also comes with a module fitted for standalone deployment, in
the form of an Akka HTTP application. Of course, this is just an
example for building a backend, and you can, in fact, use escova-core
as a dependency on your own project, and use it integrated on your own
backend.


Configuration
-------------

ESCOVA can be configured by providing a custom CostConfig value to the
routines. Depending on how you are deploying ESCOVA, you will use a
different method for configuration. 

### As an ES plugin

When installing ESCOVA as an Elasticsearch plugin, it will search for
a configuration file named "escova" and with an extension supported by
Typesafe Config. We are not using the whole Elasticsearch Settings
capabilities, as we found out that the setting datatypes were too
limiting for your possible customization needs.

We recommend that you follow our convention and call the file
`escova.conf` (remember: in the same directory as
`elasticsearch.yml`), and write the file in any valid subset
of HOCON.

### Standalone

When deploying as an Akka application, you will provide the
configuration as a standard Typesafe Config application: either
provide an application.conf file on your classpath, or override the
path to look for via a JVM System Property called `config.file` or
other supported methods by Typesafe Config.


### Configuration explanation

Configuration allows to customize the node cost and to enable
black/white -listing of certain kinds of aggregations.

You can refer to the CostConfig.scala file for reference on the
configuration.

### Config example 1

Suppose you only want to have aggregations that can have at most three
nested aggregations (one into the next). You can have a configuration
file such as:

    default.maxTreeHeight: 3

### Config example 2

Suppose, in addition to the former, you want to restrict the types of
queries that you can perform, and you want to weigh more the `term`s
than the `date_histogram`s. In this case, `date_histogram`s can only
have nested `term`s, and `term`s and `date_histogram`s are the only
allowed kinds of aggregations.

    default.maxTreeHeight: 3
	default.whitelistChildren: ["terms", "date_histogram"]
	custom.terms.defaultNodeCost: 10
	custom.date_histogram.whitelistChildren: ["terms"]


Static analysis
---------------

The first part of the analysis consists of a static analysis of the
ElasticSearch query being queried. A result with either a value of
estimated complexity or errors in the query will be presented.

If there are any syntactic errors in the query, the static analyzer
will output that.

In the case that a function is unknown to the analyzer, it will not
fail (instead, a warning will be issued), because it may be a value
existing in newer ES versions than the one implemented here, but an
issue should be opened so that it can be worked around.


Performance Validator
---------------------

For the performance validator to take place, an ElasticSearch
destination must be provided. Also, if the query is parametrized,
actual values for the parameters must be provided, as a list of
different possibilities (it is your responsability to calculate the
wanted combinations of each variable).

Given an ElasticSearch client, the query will be performed against it,
and besides returning its value, we will also monitor performance, and
balance it out against the expected outcome from the static analysis
(so that our scores do not depend *that much* on how beefy the ES
cluster is, but only on your data and your query).

That is, we will compare your query against a couple of baselines that
are meant to give some meaning to the scores.


License
-------

This software is released under the [Apache License Version
2.0](https://opensource.org/licenses/Apache-2.0), like most of
Elasticsearch is.

The code of the Escova microservice example (modules/escova-uservice/)
is released both under the [Apache License Version
2.0](https://opensource.org/licenses/Apache-2.0), as well as under the
[MIT License](https://opensource.org/licenses/MIT).

Contributing
------------

Contributions to this repository are welcome in the form of issues
or Pull Requests. By contributing, you agree to make your
contribution available under the Apache License Version 2.0.


  [esqb]: https://github.com/openshine/python-esqb/
