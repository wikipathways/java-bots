// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2015 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package org.wikipathways.reportbots;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.helixsoft.xml.Html;
import nl.helixsoft.xml.HtmlStream;

import org.bridgedb.BridgeDb;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.bio.DataSourceTxt;
import org.bridgedb.bio.Organism;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.wikipathways.webservice.WSPathway;
import org.pathvisio.wikipathways.webservice.WSPathwayInfo;
import org.wikipathways.client.WikiPathwaysClient;


public class OutdatedIdsReport {
	static Map<String, HashSet<OutdatedResult>> mapPw;
	static Map<String, HashSet<OutdatedResult>> mapRef;
	static String species;
	
	public static void main(String[] args)
			throws ClassNotFoundException, IDMapperException, ConverterException, IOException {
		
		URL wsURL = new URL("http://webservice.wikipathways.org");
		WikiPathwaysClient client = new WikiPathwaysClient(wsURL);

		

		Class.forName("org.bridgedb.rdb.IDMapperRdb");
		DataSourceTxt.init();
		IDMapper mapperNew = BridgeDb.
				connect("idmapper-pgdb:"+args[0]);
		IDMapper mapperOld = BridgeDb.
				connect("idmapper-pgdb:"+args[1]);

		WSPathwayInfo[] pathwayList = client.listPathways(Organism.fromCode(args[2]));

		File file = new File(args[3]);
		FileWriter fileWriter = new FileWriter(file);
		
		FileOutputStream str = new FileOutputStream (new File (args[4]));
		
		int count = 0;
		int cpt = 0;
		HashSet<String> setXref = new HashSet<String>();
		
		species = Organism.fromCode(args[2]).latinName();
		mapPw  =  new HashMap<String, HashSet<OutdatedResult>>();
		mapRef =  new HashMap<String, HashSet<OutdatedResult>>();
		
		for(WSPathwayInfo pathwayInfo : pathwayList) {

			WSPathway wsPathway = client.getPathway(pathwayInfo.getId());
			Pathway pathway = WikiPathwaysClient.toPathway(wsPathway);

			for(PathwayElement pwElm : pathway.getDataObjects()) {
				if(pwElm.getDataNodeType().equals("GeneProduct")) {
					if (!mapperNew.xrefExists(pwElm.getXref()) 
						&& pwElm.getXref().getDataSource()!=null 
						&& mapperOld.xrefExists(pwElm.getXref()) ){

						String pwID = pathwayInfo.getId();
						String pwName = pathwayInfo.getName();
						String refLabel = pwElm.getTextLabel().trim();
						String refID = pwElm.getXref().toString().trim();

						if (setXref.add(pwElm.getXref().getId())){
							fileWriter.write(pwID+ "\t"+ 
									pwName + "\t" + 
									refLabel + "\t" +  
									refID +"\n");
							count++;
						}
						HashSet<OutdatedResult> pwList = mapPw.get(pwID);

						if (pwList==null){
							HashSet<OutdatedResult> list = new HashSet<OutdatedResult>();
							list.add(new OutdatedResult(pwID, pwName, refLabel, refID));
							mapPw.put(pwID, list);
						}
						else{
							pwList.add(new OutdatedResult(pwID, pwName, refLabel, refID));
							cpt++;
						}	
						HashSet<OutdatedResult> xrefList = mapRef.get(refID);

						if (xrefList==null){
							HashSet<OutdatedResult> list = new HashSet<OutdatedResult>();							
							list.add(new OutdatedResult(pwID, pwName, refLabel, refID));
							mapRef.put(refID, list);
						}
						else{
							xrefList.add(new OutdatedResult(pwID, pwName, refLabel, refID));
							cpt++;
						}	
					}	
				}
			}			
		}
		System.out.println(count);
		System.out.println(cpt);
		System.out.println("Set: "+setXref.size());
		fileWriter.flush();
		fileWriter.close();
				
		printHtmlOverview(new PrintStream(str));
	}

	public static void printHtmlOverview(PrintStream stream) throws IOException
	{
		HtmlStream out = new HtmlStream(stream);
		out.begin ("html");
		out.begin ("head");
		out.add (Html.collapseScript());
		out.end ("head");

		out.begin ("body");

		out.add(Html.h1("WikiPathways - " +species + " - outdated ids for Ensembl v80"));
		out.add(Html.p(
				"For more information on how these errors are dealt with, ask Tina not Jonathan."));

		out.add (Html.p(mapRef.size()+" unique ids outdated in "+mapPw.size()+" pathways."));

		out.add (Html.p(Html.collapseDiv(
						Html.h2("By id"), 
						asList(mapRef,false))
						)
				);

		out.add (Html.p(Html.collapseDiv (
						Html.h2("By pathway"), 
						asList(mapPw,true))
						)
				);

		out.add(Html.p().style("font: small").addChild("Generated: ", new Date()));

		out.end("body");
		out.end("html");
	}
	private static Html asList(Map<String, HashSet<OutdatedResult>> map, boolean type)
			throws IOException{
		Html list = Html.ul(); // coarse list

		List<Map.Entry<String, HashSet<OutdatedResult>>> keys =
				new LinkedList<Map.Entry<String, HashSet<OutdatedResult>>>( map.entrySet());

		Collections.sort( keys, new Comparator<Map.Entry<String, HashSet<OutdatedResult>>>(){
			public int compare( Map.Entry<String, HashSet<OutdatedResult>> o1,
								Map.Entry<String, HashSet<OutdatedResult>> o2 )
			{
				return Integer.compare(o2.getValue().size(),o1.getValue().size());
			}
		} );
	      
		for (Entry<String, HashSet<OutdatedResult>> entry : keys){
			Html contents = Html.ul(); // fine list
			String italic = null;
			String title = null;
			for (OutdatedResult result : entry.getValue()){
				if (type){					
					contents.addChild(Html.li(
							result.getRefLabel() + ": ", Html.i(result.getRefID())));
				}
				else{
					if (entry.getKey().equals(result.getRefID())) 
						italic=result.getPwName();
					contents.addChild(
							Html.li(
									Html.a(result.getPwID()).
									href("http://wikipathways.org/index.php/Pathway:"+entry.getKey())
									+" (",
							Html.i(italic) + ") - ",
							Html.i(result.getRefLabel()) ));
				}
			}			
			Html id = Html.a(entry.getKey()).
					href("http://wikipathways.org/index.php/Pathway:"+entry.getKey());
			if (type){
				title = " - " +species + " - " + entry.getValue().size()+ " identifier(s) outdated";
				list.addChild (Html.li (
						Html.b(id),title, Html.br(),
						Html.i(italic), Html.br(),
						Html.collapseDiv ("Ref details...", contents)
						));				
			}
			else{
				title = " - outdated in "+ entry.getValue().size()+ " pathways";
				list.addChild (Html.li (
						Html.b(entry.getKey()),title, Html.br(),						
						Html.collapseDiv ("Pathways details...", contents)
						));
			}
		}
		return list;	
	}
}