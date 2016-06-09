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
package org.wikipathways.bots;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.bridgedb.bio.DataSourceTxt;
import org.bridgedb.bio.Organism;
import org.bridgedb.rdb.GdbProvider;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.preferences.PreferenceManager;
import org.wikipathways.bots.utils.GenerateGMT;
import org.wikipathways.bots.utils.GenerateGMT.GeneSet;

/**
 * Bot creates a GMT file 
 * Gene Set file format
 * @author martina
 *
 */
public class GMTBot extends Bot {

	private static final String PROP_GDBS = "gdb-config";
	// properties: 
	// webservice-url
	// cache-path
	// gdb-config
	// threshold
	// syscode (optional)
	// organism (optional)
	GdbProvider gdbs;
	
	public GMTBot(Properties props) throws BotException {
		super(props);
		
		File gdbFile = new File(props.getProperty(PROP_GDBS));
		System.out.println(gdbFile.exists());
		try {
			DataSourceTxt.init();
			gdbs = GdbProvider.fromConfigFile(gdbFile);
		} catch (Exception e) {
			throw new BotException(e);
		}
	}
	
	@Override
	public BotReport createReport(Collection<Result> result) {
		return null;
	}

	@Override
	public String getTagName() {
		return null;
	}

	@Override
	protected Result scanPathway(File pathwayFile) throws BotException {
		return null;
	}
	
	public static void main(String[] args) {
		try {
			Logger.log.trace("Starting GMTBot");
			PreferenceManager.init();
			Properties props = new Properties();
			props.load(new FileInputStream(new File(args[0])));
			GMTBot bot =  new GMTBot(props);
			bot.getCache().update();

			Calendar cal = Calendar.getInstance();
	        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	        String date = sdf.format(cal.getTime());
			
			File output = new File(args[1], date);
			output.mkdirs();
			
			GenerateGMT gmt = new GenerateGMT(bot.gdbs, bot.getClient(), bot.getCache());
			
			String syscode = "L";
			if(props.getProperty("syscode") != null) {
				syscode = props.getProperty("syscode");
			}

			
			String [] orgs = bot.getClient().listOrganisms();
			for(String o : orgs) {
				Organism org = Organism.fromLatinName(o);
				List<GeneSet> res = gmt.createGMTFile(bot.getCache().getFiles(), syscode, org);
				File f = new File(output, "gmt_wp_" + org.latinName().replace(" ", "_") + ".gmt");
				FileWriter writer = new FileWriter(f);
				for(GeneSet gs : res) {
					writer.write(printGeneSet(gs, date) + "\n");
				}
				writer.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
			printUsage();
		}
	}
	
	private static String printGeneSet(GeneSet gs, String date) {
		String output = gs.getPwyName() + "%WikiPathways_" +  date + "%" + gs.getPwyId() + "%" + gs.getOrg().latinName() + "\t" + "http://www.wikipathways.org/instance/" + gs.getPwyId() + "_r" + gs.getPwyRev();
		for(String g : gs.getGenes()) {
			output = output + "\t" + g;
		}
		return output;
	}

	static private void printUsage() {
		System.out.println(
			"Usage:\n" +
			"java org.pathvisio.wikipathways.bots.GMTBot propsfile reportfilename\n" +
			"Where:\n" +
			"-propsfile: a properties file containing the bot properties\n" +
			"-reportfilename: the base name of the file that will be used to write reports to " +
			"(extension will be added automatically)\n"
		);
	}
}
