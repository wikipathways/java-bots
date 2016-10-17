# java-bots
Java bots for WikiPathways 
- All bots are running regularly (usually once every day) 
- Results can be viewed here: http://www.pathvisio.org/data/bots/
- Example launch command: ```java -Xmx2048m -Dfile.encoding=UTF-8 -cp org.wikipathways.bots-3.2.2.jar org.wikipathways.bots.GMTBot gmt-bot.props gmt-output.gmt```


### Available bots:
- Missing description (adds curation tag to pathways without description)
- Missing literature references (adds curation tag to pathway without literature references)
- Unconnected lines (adds curation tag to pathway with unconnected lines)
- Missing xref (adds curation tag to pathway which contains datanodes without xref annotation)
- Invalid xrefs (generated list of pathways with xrefs that are not in the identifier mapping databases)
- GMT file generation (generates a GMT file that can be used for gene set enrichment analysis)
- RSSM file generation (generates RSSM file - complex XML file - of the complete pathway collection)
