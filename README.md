# java-bot for GMT from local GPML
Check out this branch to work with GMTBot code customized for working with a local folder of GPML.
Processes a single folder of GPML from a single organism to generate a single GMT file.

Example launch command: 
```
java -Xmx2048m -Dfile.encoding=UTF-8 -cp org.wikipathways.bots.LOCAL-4.0.0.jar org.wikipathways.bots.GMTBot gmt-bot.props gmt-output
```

### gmt-bot.props
```
webservice-url=http://webservice.wikipathways.org/
cache-path=gpml-input
threshold=90
gdb-config=/Users/alexpico/database/gdb.config
org=Bos taurus
```

Notes: 
 - `cache-path` points to your local folder containing GPML files
 - `gdb-config` cannot handle complex paths containing spaces or parens  
