test-lucene4
============

1. use GGTS for editing and browsing the source code.

2. run

  > gradle indexer  # build index
  
  > gradle searcher -Pq=license  # search with the index
  
3. check out the source code & run unit tests

  > svn checkout http://svn.apache.org/repos/asf/lucene/dev/trunk lucene_trunk
  > cd lucene_trunk  # cd to root directory
  
  > ant ivy-bootstrap  # install ivy
  
  > cd lucene/core  # cd to lucene-core
  > ant -p  # show available tasks
  > ant test  # test all

  Run individual test in Eclipse.

4. setup for Eclipse

  Change directory to the root directory.

  > ant -p  # show available tasks
  > ant eclipse  # set up for Eclipse
  
  
