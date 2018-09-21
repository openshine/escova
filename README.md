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

This software is released under the Apache License Version 2.0, like
most of Elasticsearch is.


Contributing
------------

Contributions to this repository are welcome in the form of issues
or Pull Requests. By contributing, you agree to make your
contribution available under the Apache License Version 2.0.


  [esqb]: https://github.com/openshine/python-esqb/
