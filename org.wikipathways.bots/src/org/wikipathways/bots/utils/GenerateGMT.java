// WikiPathways,
// Java bots to generate GMT file
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
package org.wikipathways.bots.utils;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapperException;
import org.bridgedb.IDMapperStack;
import org.bridgedb.Xref;
import org.bridgedb.bio.Organism;
import org.bridgedb.rdb.GdbProvider;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.wikipathways.webservice.WSCurationTag;
import org.wikipathways.client.WikiPathwaysClient;

/**
 * Generates a GMT file (gene set file format)
 * tab delimited file
 * first column = gene set name
 * second column = link to pathway
 * following columns = genes
 * 
 * @author martina
 *
 */
public class GenerateGMT {

	private GdbProvider idmp;
	private String[] includeTags = new String[] {
			"Curation:AnalysisCollection",
	};
	private WikiPathwaysClient client;
	
	public GenerateGMT(GdbProvider idmp, WikiPathwaysClient client) throws IOException {
		this.idmp = idmp;
		this.client = client;
	}
	
	public String createGMTFile(Collection<File> pathwayFiles) throws ConverterException, IDMapperException, RemoteException {
		String output = "";
		
		Set<String> includeIds = new HashSet<String>();
		for(String tag : includeTags) {
			for(WSCurationTag t : client.getCurationTagsByName(tag)) {
				includeIds.add(t.getPathway().getId());
			}
		}
		
		int count = 1;
		int size = includeIds.size();
		// gene set name / gene set URL / gene set
		for(File f : pathwayFiles) {
			String id = f.getName().substring(0, f.getName().length()-5);
			if(includeIds.contains(id)) {
				System.out.println(count + " out of  " + size + " pathways.");
				Pathway p = new Pathway();
				p.readFromXml(f, true);
				IDMapperStack stack = idmp.getStack(Organism.fromLatinName(p.getMappInfo().getOrganism()));
				
				Set<String> ids = new HashSet<String>();
				for(Xref x : p.getDataNodeXrefs()) {
					Set<Xref> res = stack.mapID(x, DataSource.getExistingBySystemCode("L"));
					for(Xref xref : res) {
						ids.add(xref.getId());
					}
				}
				if(ids.size() > 0) {
					output = output + p.getMappInfo().getMapInfoName() + "("  + p.getMappInfo().getOrganism() + ")" + "\thttp://wikipathways.org/instance/" + id;
					for(String s : ids) {
						output = output + "\t" + s;	
					}
					output = output + "\n";
				}
				count++;
			}
		}
		
		return output;
	}
}
