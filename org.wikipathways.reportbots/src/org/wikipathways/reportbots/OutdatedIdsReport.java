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
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import nl.helixsoft.xml.Html;
import nl.helixsoft.xml.HtmlStream;

import org.bridgedb.BridgeDb;
import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.DataSourceTxt;
import org.bridgedb.bio.Organism;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayElement.Comment;
import org.pathvisio.wikipathways.webservice.WSPathway;
import org.pathvisio.wikipathways.webservice.WSPathwayInfo;
import org.wikipathways.client.WikiPathwaysClient;


public class OutdatedIdsReport {
	static Map<String, HashSet<OutdatedResult>> mapPw;
	static Map<String, HashSet<OutdatedResult>> mapRef;
	static String species;
	static IDMapper mapperOld;
	static DataSource dsEn;
	
	public static void main(String[] args)
			throws ClassNotFoundException, IDMapperException, ConverterException, IOException {
		
		URL wsURL = new URL("http://webservice.wikipathways.org");
		WikiPathwaysClient client = new WikiPathwaysClient(wsURL);	

		Class.forName("org.bridgedb.rdb.IDMapperRdb");
		DataSourceTxt.init();
		dsEn = DataSource.getExistingBySystemCode("En");
		IDMapper mapperNew = BridgeDb.
				connect("idmapper-pgdb:"+args[0]);
		mapperOld = BridgeDb.
				connect("idmapper-pgdb:"+args[1]);
		
		species = args[0].substring(args[0].lastIndexOf("/")+1, args[0].lastIndexOf("/")+3);
		
		WSPathwayInfo[] pathwayList = client.listPathways(Organism.fromCode(species));

		File file = new File(args[3]);
		FileWriter fileWriter = new FileWriter(file);
		
		FileOutputStream str = new FileOutputStream (new File (args[4]));
		
		int count = 0;
		HashSet<String> setXref = new HashSet<String>();
		
		species = Organism.fromCode(species).latinName();
		
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
						}	
						HashSet<OutdatedResult> xrefList = mapRef.get(refID);

						if (xrefList==null){
							HashSet<OutdatedResult> list = new HashSet<OutdatedResult>();							
							list.add(new OutdatedResult(pwID, pwName, refLabel, refID));
							mapRef.put(refID, list);
						}
						else{
							xrefList.add(new OutdatedResult(pwID, pwName, refLabel, refID));
						}	
					}	
				}
			}			
		}
		System.out.println(count);
		System.out.println("Set: "+setXref.size());
		fileWriter.flush();
		fileWriter.close();
				
		printHtmlOverview(new PrintStream(str));
	}
	
	public static void run(String pathOld, String pathNew, String pathReport) 
			throws ClassNotFoundException, IDMapperException,  IOException, ConverterException {

		URL wsURL = new URL("http://webservice.wikipathways.org");
		WikiPathwaysClient client = new WikiPathwaysClient(wsURL);	

		Class.forName("org.bridgedb.rdb.IDMapperRdb");
		DataSourceTxt.init();
		dsEn = DataSource.getExistingBySystemCode("En");
		IDMapper mapperNew = BridgeDb.
				connect("idmapper-pgdb:"+pathNew);
		mapperOld = BridgeDb.
				connect("idmapper-pgdb:"+pathOld);
		
		species = pathOld.substring(pathOld.lastIndexOf("/")+1, pathOld.lastIndexOf("/")+3);
		
		WSPathwayInfo[] pathwayList = client.listPathways(Organism.fromCode(species));

		File file = new File(pathReport+".txt");
		FileWriter fileWriter = new FileWriter(file);
		
		FileOutputStream str = new FileOutputStream (new File (pathReport+".html"));
		
		int count = 0;
		HashSet<String> setXref = new HashSet<String>();
		
		species = Organism.fromCode(species).latinName();
		
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
						}	
						HashSet<OutdatedResult> xrefList = mapRef.get(refID);

						if (xrefList==null){
							HashSet<OutdatedResult> list = new HashSet<OutdatedResult>();							
							list.add(new OutdatedResult(pwID, pwName, refLabel, refID));
							mapRef.put(refID, list);
						}
						else{
							xrefList.add(new OutdatedResult(pwID, pwName, refLabel, refID));
						}	
					}	
				}
			}			
		}
		System.out.println(count);
		System.out.println("Set: "+setXref.size());
		fileWriter.flush();
		fileWriter.close();
				
		printHtmlOverview(new PrintStream(str));
	}

	public static void printHtmlOverview(PrintStream stream) throws IOException, IDMapperException
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
			throws IOException, IDMapperException{
		Html list = Html.ul();

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
			Html contents = Html.ul();
			String italic = null;
			String title = null;
			for (OutdatedResult result : entry.getValue()){
				if (type){
					if (entry.getKey().equals(result.getPwID())) 
						italic=result.getPwName();
					contents.addChild(Html.li(
							result.getRefLabel() + ": ", Html.i(result.getRefID())));
				}
				else{
					if (entry.getKey().equals(result.getRefID())) 
						italic=result.getPwName();
					String href = "<a href='http://wikipathways.org/index.php/Pathway:"+result.getPwID()
							+ "' target='_null'>"+result.getPwID()+"</a>";
					contents.addChild(
							Html.li(href+" (",
							Html.i(italic) + ") - ",
							Html.i(result.getRefLabel()) ));
				}
			}
			if (type){
				String href = "<a href='http://wikipathways.org/index.php/Pathway:"+entry.getKey()
						+ "' target='_null'>"+entry.getKey()+"</a>";
				title = " - " +species + " - " + entry.getValue().size()+ " identifier(s) outdated";
				list.addChild (Html.li (
						Html.b(href),title, Html.br(),
						Html.i(italic), Html.br(),
						Html.collapseDiv ("Ref details...", contents)
						));				
			}
			else{
				Set<Xref> ensRef = new HashSet<Xref> ();
				if (!entry.getKey().contains("En")){
					String id = entry.getKey().substring(3, entry.getKey().length());
					DataSource ds = DataSource.getExistingBySystemCode(entry.getKey().substring(0, entry.getKey().indexOf(":")));
					Xref ref = new Xref(id,ds);
					ensRef = mapperOld.mapID(ref, dsEn);
				}
				title = " "+ensRef+" - outdated in "+ entry.getValue().size()+ " pathways";
				list.addChild (Html.li (
						Html.b(entry.getKey()),title, Html.br(),						
						Html.collapseDiv ("Pathways details...", contents)
						));
			}
		}
		return list;	
	}

	static Map<String, HashSet<OutdatedResult>> mapIds = new HashMap<String, HashSet<OutdatedResult>>();
	static Map<String, HashSet<OutdatedResult>> allMapIds = new HashMap<String, HashSet<OutdatedResult>>();

	public static void runAll(String pathOld, String pathNew) 
			throws ClassNotFoundException, IDMapperException,  IOException, ConverterException {

		URL wsURL = new URL("http://webservice.wikipathways.org");
		WikiPathwaysClient client = new WikiPathwaysClient(wsURL);	

		Class.forName("org.bridgedb.rdb.IDMapperRdb");
		DataSourceTxt.init();
		dsEn = DataSource.getExistingBySystemCode("En");
		IDMapper mapperNew = BridgeDb.
				connect("idmapper-pgdb:"+pathNew);
		mapperOld = BridgeDb.
				connect("idmapper-pgdb:"+pathOld);
		
		species = pathOld.substring(pathOld.lastIndexOf("/")+1, pathOld.lastIndexOf("/")+3);
		
		WSPathwayInfo[] pathwayList = client.listPathways(Organism.fromCode(species));
		
		species = Organism.fromCode(species).latinName();
		
		
		for(WSPathwayInfo pathwayInfo : pathwayList) {

			WSPathway wsPathway = client.getPathway(pathwayInfo.getId());
			boolean flag = true;
			Pathway pathway = WikiPathwaysClient.toPathway(wsPathway);
			for ( Comment c : pathway.getMappInfo().getComments()){
				if (c.getSource()!=null && c.getSource().equals("HomologyConvert")){
					flag=false;
				}
			}
			if (flag){
				for(PathwayElement pwElm : pathway.getDataObjects()) {
						if (!mapperNew.xrefExists(pwElm.getXref()) 
								&& pwElm.getXref().getDataSource()!=null 
								&& mapperOld.xrefExists(pwElm.getXref()) ){

							String pwID = pathwayInfo.getId();
							String pwName = pathwayInfo.getName();
							String refLabel = pwElm.getTextLabel().trim();
							String refID = pwElm.getXref().toString().trim();

							HashSet<OutdatedResult> xrefList = allMapIds.get(refID);


							Set<Xref> ensOld = new HashSet<Xref>();

							if (!pwElm.getXref().getDataSource().equals(dsEn)){
								ensOld = mapperOld.mapID(pwElm.getXref(), dsEn);
							}

							if (xrefList==null){
								HashSet<OutdatedResult> list = new HashSet<OutdatedResult>();							
								list.add(new OutdatedResult(pwID, pwName, refLabel, refID, ensOld, species));
								allMapIds.put(refID, list);
							}
							else{
								xrefList.add(new OutdatedResult(pwID, pwName, refLabel, refID, ensOld, species));
							}	
						}	
				}
			}
		}
	}
	
	public static void writeAll(String name) throws IOException, IDMapperException{
		List<Map.Entry<String, HashSet<OutdatedResult>>> keys =
				new LinkedList<Map.Entry<String, HashSet<OutdatedResult>>>( allMapIds.entrySet());	
		File file = new File(name);
		FileWriter fileWriter = new FileWriter(file);
		
		for (Entry<String, HashSet<OutdatedResult>> entry : keys){				
			for (OutdatedResult result : entry.getValue()){				
				fileWriter.write(
					result.getRefLabel().trim().replaceAll("\\s+","")+"\t"+
					result.getPwID()+"\t"+
					entry.getKey()+"\t"+
					result.getEnsOld()+"\t"+
					result.getSpecies()+"\n"
					);
			}
		}
		fileWriter.flush();
		fileWriter.close();
		System.out.println(allMapIds.size());
	}
}