[![Build Status](https://travis-ci.org/n-k/mysqlreplicator.svg?branch=master)](https://travis-ci.org/n-k/mysqlreplicator)

# Mysql to elasticsearch replicator

Connects to mysql as a replication slave and reads binlog events.


Changed/added rows are converted to json, then inserted into elasticsearch.


Strategy for creation of document ids can be configured to use primary key in mysql
