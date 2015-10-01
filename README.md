Mysql to elasticsearch replicator

Connects to mysql as a replication slave and reads binlog events.


Changed/added rows are converted to json, then inserted into elasticsearch.


Strategy for creation of document ids can be configured to use primary key in mysql


Supports saving binlog position for next run or to recover from crashes.



