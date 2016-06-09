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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
import org.pathvisio.wikipathways.webservice.WSPathwayInfo;
import org.wikipathways.client.WikiPathwaysCache;
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
	private WikiPathwaysCache cache;
	
	public GenerateGMT(GdbProvider idmp, WikiPathwaysClient client, WikiPathwaysCache cache) throws IOException {
		this.idmp = idmp;
		this.client = client;
		this.cache = cache;
	}
	
	public List<GeneSet> createGMTFile(Collection<File> pathwayFiles, String syscode, Organism organism) throws ConverterException, IDMapperException, FileNotFoundException, IOException {
		
		List<GeneSet> output = new ArrayList<GeneSet>();
		
		Set<String> includeIds = new HashSet<String>();
		for(String tag : includeTags) {
			for(WSCurationTag t : client.getCurationTagsByName(tag)) {
				if(t.getPathway().getSpecies().equals(organism.latinName())) {
					includeIds.add(t.getPathway().getId());
				}
			}
		}
		
		System.out.println(includeIds.size());
		
		int count = 1;
		int size = includeIds.size();

		for(File f : pathwayFiles) {
			WSPathwayInfo i = cache.getPathwayInfo(f);
			if(includeIds.contains(i.getId())) {
				Pathway p = new Pathway();
				p.readFromXml(f, true);
				Organism org = Organism.fromLatinName(i.getSpecies());
				IDMapperStack stack = idmp.getStack(org);
				GeneSet gs = new GeneSet(org, p, i.getId(), i.getRevision());
				
				System.out.println(count + " out of  " + size + " pathways.");
				
				for(Xref x : p.getDataNodeXrefs()) {
					Set<Xref> res = stack.mapID(x, DataSource.getExistingBySystemCode(syscode));
					for(Xref xref : res) {
						gs.getGenes().add(xref.getId());
					}
				}
				
				if(gs.getGenes().size() > 0) { 
					output.add(gs); 
				}
				count++;
			}
		}
		
		return output;
	}
	
	public class GeneSet {
		private Organism org;
		private Pathway pwy;
		private String pwyId;
		private String pwyRev;
		private Set<String> genes;
		private String pwyName;
		
		public GeneSet(Organism org, Pathway pwy, String pwyId, String pwyRev) {
			this.org = org;
			this.pwy = pwy;
			this.pwyId = pwyId;
			this.pwyRev = pwyRev;
			pwyName = pwy.getMappInfo().getMapInfoName();
			genes = new HashSet<String>();
		}

		public Organism getOrg() {
			return org;
		}

		public Pathway getPwy() {
			return pwy;
		}

		public String getPwyId() {
			return pwyId;
		}

		public String getPwyRev() {
			return pwyRev;
		}

		public Set<String> getGenes() {
			return genes;
		}
		
		public String getPwyName() {
			return pwyName;
		}
	}
}
