// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2014-2015 BiGCaT Bioinformatics
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
package org.wikipathways.reportbots.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import nl.helixsoft.xml.Html;
import nl.helixsoft.xml.HtmlStream;

import org.bridgedb.IDMapperException;
import org.bridgedb.IDMapperStack;
import org.bridgedb.Xref;
import org.bridgedb.bio.DataSourceTxt;
import org.bridgedb.bio.Organism;
import org.bridgedb.rdb.GdbProvider;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.wikipathways.webservice.WSPathwayInfo;
import org.wikipathways.reportbots.OutdatedResult;
import org.wikipathways.reportbots.test.Bot.Result;

/**
 * Checks if a datanode is annotated with an Xref that is 
 * not supported by the identifier mapping databases
 * Replaces old Xref bot together with MissingAnnotationBot
 * @author mkutmon
 */
public class InvalidAnnotationBot extends Bot {
	private static final String CURATIONTAG = "Curation:MissingXRef";
	private static final String PROP_THRESHOLD = "threshold";
	private static final String PROP_GDBS = "gdb-config";
	
	static HashMap<String, HashSet<InvalidResult>> mapPw;
	static HashMap<String, HashSet<InvalidResult>> mapRef;

	GdbProvider gdbs;
	double threshold;

	public InvalidAnnotationBot(Properties props) throws BotException {
		super(props);
		String thr = props.getProperty(PROP_THRESHOLD);
		if(thr != null) threshold = Double.parseDouble(thr);
		
		File gdbFile = new File(props.getProperty(PROP_GDBS));
		try {
			DataSourceTxt.init();
			gdbs = GdbProvider.fromConfigFile(gdbFile);
		} catch (Exception e) {
			System.out.println(props.getProperty(PROP_GDBS));
			System.out.println(gdbFile.getAbsolutePath());
			throw new BotException(e);
		}
	}

	public String getTagName() {
		return CURATIONTAG;
	}

	public BotReport createReport(Collection<Result> results) {
		BotReport report = new BotReport(
			new String[] {
				"Nr Xrefs", "Nr invalid", "% invalid", "Invalid Annotations"
			}
		);
		report.setTitle("InvalidAnnotationBot scan report");
		report.setDescription("The InvalidAnnotationBot checks for invalid DataNode annotations");
		
		mapPw  =  new HashMap<String, HashSet<InvalidResult>>();
		mapRef =  new HashMap<String, HashSet<InvalidResult>>();
		
		for(Result r : results) {
			XRefResult xr = (XRefResult)r;
			HashSet<PathwayElement> set = new HashSet<PathwayElement>(xr.xrefs.keySet());
			
			String pwID = r.getPathwayInfo().getId();
			int nbInvalid = xr.getNrInvalid();
			if (nbInvalid>0){
				HashSet<InvalidResult> pwList = mapPw.get(pwID);
				if (pwList==null){
					HashSet<InvalidResult> list = new HashSet<InvalidResult>();
					for(PathwayElement pe : set){
						list.add(new InvalidResult(r.getPathwayInfo(),pe,nbInvalid ));
					}				
					mapPw.put(pwID, list);
				}
				else{
					for(PathwayElement pe : set){
						pwList.add(new InvalidResult(r.getPathwayInfo(),pe,nbInvalid ));
					}

				}	
				for(PathwayElement pe : set){
					if(!xr.xrefs.get(pe)) {
						HashSet<InvalidResult> xrefList = mapRef.get(pe);
						if (xrefList==null){
							HashSet<InvalidResult> list = new HashSet<InvalidResult>();							
							list.add(new InvalidResult(r.getPathwayInfo(),pe,nbInvalid ));
							mapRef.put(pe.getXref().toString(), list);
						}
						else{
							xrefList.add(new InvalidResult(r.getPathwayInfo(),pe,nbInvalid ));
						}
					}	
				}
			}
		}
		return report;
	}

	protected Result scanPathway(File pathwayFile) throws BotException {
		try {
			XRefResult report = new XRefResult(getCache().getPathwayInfo(pathwayFile));
//			XRefResult report = new XRefResult(getClient().getPathwayInfo("WP2152"));
			Pathway pathway = new Pathway();
			pathway.readFromXml(pathwayFile, true);
			
			String orgName = pathway.getMappInfo().getOrganism();
			Organism org = Organism.fromLatinName(orgName);
			if(org == null) org = Organism.fromShortName(orgName);

			for(PathwayElement pwe : pathway.getDataObjects()) {
				if(pwe.getObjectType() == ObjectType.DATANODE) {
					boolean valid = true;
					Xref xref = pwe.getXref();
					IDMapperStack gdb = gdbs.getStack(org);
					try {
						if(xref.getId() != null && xref.getDataSource() != null) {
							if(!gdb.xrefExists(xref)) {
								valid = false;
							}
						}
					} catch (IDMapperException e) {
						Logger.log.error("Error checking xref exists", e);
					}
					report.addXref(pwe, valid);
				}
			}

			return report;
		} catch(Exception e) {
			throw new BotException(e);
		}
	}

	private class XRefResult extends Result {
		Map<PathwayElement, Boolean> xrefs = new HashMap<PathwayElement, Boolean>();

		public XRefResult(WSPathwayInfo pathwayInfo) {
			super(pathwayInfo);
		}

		public boolean shouldTag() {
			return getPercentValid() < threshold;
		}

		public boolean equalsTag(String tag) {
			return getTagText().equals(tag);
		}

		public void addXref(PathwayElement pwe, boolean valid) {
			if (!pwe.getXref().toString().equals(":") && valid==false)
				xrefs.put(pwe, valid);
		}

		public int getNrXrefs() {
			return xrefs.size();
		}

		public double getPercentValid() {
			return (double)(100 * getNrValid()) / getNrXrefs();
		}

		public double getPercentInvalid() {
			return (double)(100 * getNrInvalid()) / getNrXrefs();
		}

		public int getNrInvalid() {
			return getNrXrefs() - getNrValid();
		}

		public int getNrValid() {
			int v = 0;
			for(PathwayElement pwe : xrefs.keySet()) {
				if(xrefs.get(pwe)) {
					v++;
				}
			}
			return v;
		}

		public List<String> getLabelsForInvalid() {
			List<String> labels = new ArrayList<String>();
			for(PathwayElement pwe : xrefs.keySet()) {
				if(!xrefs.get(pwe)) {
					labels.add(pwe.getTextLabel() + "[" + pwe.getXref() + "]");
				}
			}
			return labels;
		}

		private String[] getLabelStrings() {
			List<String> labels = getLabelsForInvalid();
			Collections.sort(labels);
			String labelString = "";
			String labelStringTrun = "";
			for(int i = 0; i < labels.size(); i++) {
				labelString += labels.get(i) + ", ";
				if(i < 3) {
					labelStringTrun += labels.get(i) + ", ";
				} else if(i == 3) {
					labelStringTrun += " ..., ";
				}
			}
			if(labelString.length() > 2) {
				labelString = labelString.substring(0, labelString.length() - 2);
			}
			if(labelStringTrun.length() > 2) {
				labelStringTrun = labelStringTrun.substring(0, labelStringTrun.length() - 2);
			}
			return new String[] { labelString, labelStringTrun };
		}

		public String getTagText() {
			String[] labels = getLabelStrings();

			//Limit length of label string
			if(labels[0].length() > 300) {
				labels[0] = labels[0].substring(0, 300) + "...";
			}
			String txt = getNrInvalid() + " out of " + getNrXrefs() +
				" DataNodes have an incorrect external reference: " +
				"<span title=\"" + labels[0] + "\">" + labels[1] + "</span>";
			return txt;
		}
	}

	public static void main(String[] args) {
		try {
			Logger.log.trace("Starting InvalidAnnotationBot");
			Properties props = new Properties();
			props.load(new FileInputStream(new File(args[0])));
			InvalidAnnotationBot bot = new InvalidAnnotationBot(props);
			
			Logger.log.trace("Running bot " + bot);
			Collection<Result> results = bot.scan();
			
//			File file = new File("/home/bigcat-jonathan/WP2152_77496.gpml");
//			Logger.log.trace("Running bot " + bot);		
//			List<Result> reports = new ArrayList<Result>();
//			reports.add(bot.scanPathway(file));
//			Collection<Result> results = reports;

			Logger.log.trace("Generating report");
			BotReport report = bot.createReport(results);

//			Logger.log.trace("Writing text report");
//			report.writeTextReport(new File(args[1] + ".txt"));
//
//			Logger.log.trace("Writing HTML report");
//			report.writeHtmlReport(new File(args[1] + ".html"));
			
			FileOutputStream str = new FileOutputStream (new File (args[1]));
			printHtmlOverview(new PrintStream(str));
		} catch(Exception e) {
			e.printStackTrace();
			printUsage();
		}
	}

	static private void printUsage() {
		System.out.println(
			"Usage:\n" +
			"java org.pathvisio.wikipathways.bots.InvalidAnnotationBot propsfile reportfilename\n" +
			"Where:\n" +
			"-propsfile: a properties file containing the bot properties\n" +
			"-reportfilename: the base name of the file that will be used to write reports to " +
			"(extension will be added automatically)\n"
		);
	}
	static public void printHtmlOverview(PrintStream stream) throws IOException
	{
		HtmlStream out = new HtmlStream(stream);
		out.begin ("html");
		out.begin ("head");
		out.add (Html.collapseScript());
		out.end ("head");

		out.begin ("body");

		out.add(Html.h1("InvalidAnnotationBot scan report"));
		out.add(Html.p(
				"For more information on how these errors are dealt with, ask Tina not Jonathan."));

		

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
	static private  Html asList(HashMap<String, HashSet<InvalidResult>> data2, boolean type)
			throws IOException{
		Html list = Html.ul();

		List<Map.Entry<String, HashSet<InvalidResult>>> keys =
				new LinkedList<Map.Entry<String, HashSet<InvalidResult>>>(data2.entrySet());

		Collections.sort( keys, new Comparator<Map.Entry<String, HashSet<InvalidResult>>>(){
			public int compare( Map.Entry<String, HashSet<InvalidResult>> o1,
								Map.Entry<String, HashSet<InvalidResult>> o2 )
			{
//				return o1.getKey().compareTo(o2.getKey());
				return Integer.compare(o2.getValue().size(),o1.getValue().size());
			}
		} );
	      
		for (Entry<String, HashSet<InvalidResult>> entry : keys){
			Html contents = Html.ul();
//			String italic = null;
			String title = null;
			WSPathwayInfo path = null;
			for (InvalidResult result : entry.getValue()){
//				for ( PathwayElement pe : result.getListRef()){
					if (type){
						if (entry.getKey().equals(result.getPwInfo().getId())) {
							path=result.getPwInfo();
							
						}
						contents.addChild(Html.li(
								result.getPe().getTextLabel().toString().trim() + ": ",
								Html.i(result.getPe().getXref().toString())));
					}
					else{
//						if (entry.getKey().equals(pe.getXref().toString())) {
//							italic=result.getPwInfo().getName();
							String href = "<a href='http://wikipathways.org/index.php/Pathway:"+result.getPwInfo().getId()
									+ "' target='_null'>"+result.getPwInfo().getId()+"</a>";
							contents.addChild(
									Html.li(href+" (",
											Html.i(result.getPwInfo().getName()) + ") - ",
											Html.i(result.getPe().getTextLabel().trim()) ));
//						}
					}
				}
			
			if (type){
				String href = "<a href='http://wikipathways.org/index.php/Pathway:"+entry.getKey()
						+ "' target='_null'>"+entry.getKey()+"</a>";
				String name = "";
				title = " - ";
				if (path!=null){
					title = " - "+path.getSpecies()+" - "+entry.getValue().size()
							+" identifier(s) invalid";
					name = path.getName();
				}
				

				list.addChild (Html.li (
						Html.b(href),title, Html.br(),
						Html.i(name), Html.br(),
						Html.collapseDiv ("Ref details...", contents)
						));				
			}
			else{
				title = " - invalid in "+ entry.getValue().size()+ " pathways";
				list.addChild (Html.li (
						Html.b(entry.getKey()),title, Html.br(),						
						Html.collapseDiv ("Pathways details...", contents)
						));
			}
		}
		return list;	
	}
}
